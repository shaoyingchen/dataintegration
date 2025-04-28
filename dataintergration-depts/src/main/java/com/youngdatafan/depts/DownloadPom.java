package com.youngdatafan.depts;

import cn.hutool.http.HttpUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// https://repo.spring.io/ui/native/plugins-release/org/springframework/cloud/spring-cloud-dependencies/2020.0.0-M6/
// /work/home/develop/projects/data-integration/_repository
public class DownloadPom {

    final static String USER_DIR = System.getProperty("user.dir") + File.separator;
    final static String REPOSITORY = USER_DIR + "_repository" + File.separator;

    // https://repo.spring.io/ui/api/v1/download?repoKey=plugins-release&path=org%252Fspringframework%252Fcloud%252Fspring-cloud-dependencies%252F2020.0.0-M6%252Fspring-cloud-dependencies-2020.0.0-M6.pom&isNativeBrowsing=true
    final static String BASE_URL = "https://repo.spring.io/ui/api/v1/download?repoKey=plugins-release&path={path}&isNativeBrowsing=true";

    public static void main(String[] args) throws IOException, DocumentException {
        String pom = DownloadPom.class.getClassLoader().getResource("spring-cloud-dependencies-2020.0.0-M6.pom").getFile();
        analyzePom(pom, true);

    }

    private static void copyErrorDownload(){

    }

    private static void scanAnalyzePom() throws DocumentException {
        File root = new File("/work/home/develop/projects/data-integration/_repository/org/springframework/cloud/");
        List<String> all = new ArrayList<>();
        getFiles(root, all);

        for (String file : all) {
            analyzePom(file, false);
        }
    }

    private static void analyzePom(String pomPath, boolean isDeep) throws DocumentException {
        File file = new File(pomPath);
//        String s = FileUtils.readFileToString(file);
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);
        Element root = document.getRootElement();
        Element properties = root.element("properties");
        Element dependenciesEle = root.element("dependencies");
        if (dependenciesEle == null) {
            if (root.element("dependencyManagement") == null) return;
            dependenciesEle = root.element("dependencyManagement").element("dependencies");
        }
        List<Element> dependencies = dependenciesEle.elements("dependency");

        Element parentEle = root.element("parent");
        Element packaging = root.element("packaging");
        Element pVersionEle = root.element("version");

        List<String> poms = new ArrayList<>();
        for (Element element : dependencies) {
            String groupId = element.element("groupId").getText();
            String artifactId = element.element("artifactId").getText();
            Element versionEle = element.element("version");
            String version = null;
            if (versionEle != null) {
                version = versionEle.getText().replaceAll("[${}]", "");
                if (properties != null && properties.element(version) != null) {
                    version = properties.element(version).getText();
                } else if (pVersionEle != null && version.matches("(project\\.)?version")) {
                    version = pVersionEle.getText();
                }
            }
            if (version == null && parentEle != null) {
                version = parentEle.element("version").getText();
            }

            if (version == null) {
                System.out.printf("ERROR: %s %s %s%n", pomPath, groupId, artifactId);
                continue;
            }
            String type = packaging == null ? "jar" : packaging.getText(); //element.element("type").getText();

            List<String> r = download(groupId, artifactId, version, type);
            poms.addAll(r);

            // 下载parent
            if (parentEle != null) {
                Element g = parentEle.element("groupId");
                Element a = parentEle.element("artifactId");
                Element v = parentEle.element("version");
                if (g != null && a != null && v != null) {
                    List<String> r1 = download(g.getText(), a.getText(), v.getText(), "pom");
                    poms.addAll(r1);
                }
            }

        }

        if (!isDeep) return;
        for (String pom : poms) {
            analyzePom(pom, true);
        }
    }

    private static List<String> download(String groupId, String artifactId, String version, String type) {

        String dirname = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/";
        String filename = artifactId + "-" + version + "." + type;

        if (!filename.matches(".*-M\\d+\\.(pom|jar)$")) {
            return Collections.EMPTY_LIST;
        }

        List<String> poms = new ArrayList<>();

        String path = (dirname + filename).replace("/", "%252F");
        String url = BASE_URL.replace("{path}", path);
        System.out.println(url);

        File dir = new File(REPOSITORY + dirname);
        File distFile = new File(dir.getAbsolutePath() + File.separator + filename);
        if (!dir.exists()) dir.mkdirs();
        if (!distFile.exists()) {
            try {
                HttpUtil.downloadFile(url, distFile);
                if (filename.endsWith(".pom")) poms.add(distFile.getAbsolutePath());
                String savePath = distFile.getAbsolutePath();
                boolean isJar = type.endsWith("jar");
                // 下载jar的pom或者pom的jar
                try {
                    String newPath = isJar ? savePath.replaceAll("\\.jar$", ".pom") : savePath.replaceAll("\\.pom$", ".jar");
                    if (!new File(newPath).exists()) {
                        String jarPom = isJar ? url.replaceAll("\\.jar&", ".pom&") : url.replaceAll("\\.pom&", ".jar&");
                        System.out.println(jarPom);
                        HttpUtil.downloadFile(jarPom, newPath);
                        if (newPath.endsWith(".pom")) poms.add(newPath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return poms;
    }

    private static void getFiles(File root, List<String> all) {
        if (root == null || root.isFile()) return;
        for (File file : Objects.requireNonNull(root.listFiles())) {
            if (file.isFile() && !file.isHidden() && file.getAbsolutePath().matches(".+\\.(pom)$")) {
                all.add(file.getAbsolutePath());
            } else {
                getFiles(file, all);
            }
        }
    }

}
