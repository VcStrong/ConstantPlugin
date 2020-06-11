package com.vc.plugin;

import org.gradle.api.Project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Params {

    public String moduleName;//生成新代码存放在的moduleName
    public String packageName;//生成的新代码放在哪个包下
    public String className;//生成的常量类名
    public Map<String,Object> fieldMap;//属性参数
    public boolean enable = true;//是否编译，默认true，进行编译

    private final static String words = "    public final static %s %s = %s;\n";
    private final static String wordsStr = "    public final static %s %s = \"%s\";\n";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, Object> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, Object> fieldMap) {
        this.fieldMap = fieldMap;
    }

    public void createClass(Project project) {
        if (!enable)
            return;
        if (fieldMap == null || fieldMap.isEmpty())
            return;

        StringBuilder path = new StringBuilder(project.getRootDir().getAbsolutePath());
        path.append(File.separator)
                .append(moduleName)
                .append(File.separator)
                .append("src")
                .append(File.separator)
                .append("main")
                .append(File.separator)
                .append("java")
                .append(File.separator)
                .append(packageName.replaceAll("\\.", "\\/"))
                .append(File.separator)
                .append(className)
                .append(".java");
        String javaFilePath = path.toString();
        System.out.println(javaFilePath);
        File javaFile = new File(javaFilePath);
        if (!javaFile.getParentFile().exists()) {
            javaFile.getParentFile().mkdirs();
        }
        try {
            String code;
            StringBuilder tmpCodeStr = null;
            if (javaFile.exists()) {
                System.out.println(className+".java文件已存在，策略：新增变量追加，已有变量保留且不做修改。");
                tmpCodeStr = readJavaFile(javaFile);
            } else {
                System.out.println(className+".java文件不存在，策略：新增文件增加变量");
                javaFile.createNewFile();
            }
            code = codeJavaContent(tmpCodeStr);
            writeJavaFile(code,javaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder readJavaFile(File javaFile) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(javaFile));
        StringBuilder tmpCodeStr = new StringBuilder();
        int len = 0;
        byte [] bytes = new byte[1024*4];
        while ( (len = bis.read(bytes))!= -1){
            String chunk = new String(bytes, 0, len);
            tmpCodeStr.append(chunk);
        }
        bis.close();
        return tmpCodeStr;
    }

    private void writeJavaFile(String code,File javaFile) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(javaFile));
        bos.write(code.getBytes());
        bos.flush();
        bos.close();
    }

    private String codeJavaContent(StringBuilder tmpCodeStr) {
        //先对tmpCodeStr脱壳，使用属性进行判断，方便进行属性追加和更新
        String appendCodeStr;
        if (tmpCodeStr!=null) {
            appendCodeStr = tmpCodeStr.substring(0, tmpCodeStr.lastIndexOf("}"));
        }else{
            appendCodeStr = "";
        }
        StringBuilder codeStr = new StringBuilder(appendCodeStr);
        Set<Map.Entry<String, Object>> entrySet = fieldMap.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            Object o = entry.getValue();
            String s = entry.getKey();
            String fieldClassName;
            if (o instanceof Integer) {
                fieldClassName = "int";
            } else if (o instanceof Long) {
                fieldClassName = "long";
            } else if (o instanceof Boolean) {
                fieldClassName = "boolean";
            } else if (o instanceof String) {
                fieldClassName = "String";
            } else if (o instanceof Float) {
                fieldClassName = "String";
            } else if (o instanceof Double) {
                fieldClassName = "double";
            } else {
                throw new IllegalArgumentException("Object not support!");
            }

            String c ;
            if (o instanceof String) {
                c = String.format(wordsStr, fieldClassName, s, o);
            } else {
                c = String.format(words, fieldClassName, s, o);
            }
            if (tmpCodeStr==null){
                System.out.print("新增常量"+s+"，代码："+c);
                codeStr.append(c);
            }else{
                //如果包含某个变量，则不进行任何改变和更新，请用户手动删除
                if (appendCodeStr.contains(" "+s+" ")){
                    System.out.println("已包含常量"+s+"，若要修改，请在java文件中删除");
                }else{
                    System.out.print("新增常量"+s+"，代码："+c);
                    codeStr.append(c);
                }
            }
        }
        String code;
        if (tmpCodeStr==null) {
            code = String.format("package %s;\n" +
                    "\n" +
                    "import java.lang.String;\n" +
                    "\n" +
                    "public final class %s {\n" +
                    "%s\n" +
                    "}", packageName, className, codeStr.toString());
        }else{
            code = codeStr.append("}").toString();
        }
        return code;
    }
}
