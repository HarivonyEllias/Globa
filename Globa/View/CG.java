import java.sql.Statement;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import com.google.gson.Gson;

public class CG {
   public static Connection establishConnection(String base,String ConnectionString,String user,String pass) throws Exception{
      String driver = getDriver(base);
      try {
         Class.forName(driver);
      } catch (ClassNotFoundException e) {
            throw new RuntimeException(base+" JDBC driver not found. Make sure it's in your classpath.", e);
      }
      return DriverManager.getConnection(ConnectionString, user, pass);
   } 

   public static ResultSetMetaData getDatabaseMetadata(Connection connection,String tableName, String database) throws SQLException {

      String query = "SELECT * FROM " + tableName + " WHERE 1=0";
          try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
              ResultSet resultSet = preparedStatement.executeQuery();
              ResultSetMetaData metaData = resultSet.getMetaData();
              System.out.println(metaData.getColumnCount());
              return metaData;
          }
   }   

   public static String getDriver(String base) throws Exception{
       Connection con = PGSQLConnection.getConnection();
       String query = "SELECT driver FROM bdd WHERE bdd=?";
          try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
            preparedStatement.setString(1, base.toLowerCase());
              ResultSet resultSet = preparedStatement.executeQuery();
              if(resultSet.next()){
                  System.out.println(resultSet.getString("driver"));
                 return resultSet.getString("driver");
              }
          }
      return "Driver not found";
   }

   public static String getTemplateCode(String filedirectory) throws Exception{
      StringBuilder content = new StringBuilder();
      try (BufferedReader br = new BufferedReader(new FileReader(filedirectory))) {
            String line;
            while ((line = br.readLine()) != null) {
               content.append(line).append("\n"); // Append each line with a newline character
            }
      }
      return content.toString();
   }

   public static String getsetgenerator(String filename) throws Exception{
      StringBuilder content = new StringBuilder();
      try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
               content.append(line).append("\n");
            }
      }
      return content.toString();
   }

   public static HashMap<String,String> getVocabs(String language) throws Exception {
       Connection con = PGSQLConnection.getConnection();
       String query = "select vocabulaire from syntaxe where language = ?";
          try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
            preparedStatement.setString(1, language.toLowerCase());
              ResultSet resultSet = preparedStatement.executeQuery();
              if(resultSet.next()){
                  Gson g = new Gson();

                 return g.fromJson(resultSet.getString("vocabulaire"), HashMap.class);
              }
          }
      return null;
   }

   public static String getExtension(String language)throws Exception{
      return getVocabs(language).get("extension");
   }
   
   public static HashMap<String, String> generateFileStructure(String language, String base, String tableName,
      String templateDirectory, String pkg, String connectionString,
      String user, String pass) throws Exception {
      HashMap<String, String> fileStructure = new HashMap<>();
      Connection con = establishConnection(base, connectionString, user, pass);
      HashMap<String,String> vocabs = getVocabs(language);
      String templateCode = getTemplateCode(templateDirectory);
      for (Entry<String, String> entry : vocabs.entrySet()) {
         String key = entry.getKey();
         String value = entry.getValue();
         templateCode = templateCode.replace(key, value);
      }
      ResultSetMetaData metadata = getDatabaseMetadata(con, tableName, base);
      int columnCount = metadata.getColumnCount();
      Type typeMapping = new Type("Type_" + language + "_" + base + ".props");
      String getsettemplate = getsetgenerator("getter_setter_" + language+".txt" );
      StringBuilder listAttributes = new StringBuilder();
      for (int i = 1; i <= columnCount; i++) {
         String typeLanguage =  typeMapping.getType(metadata.getColumnTypeName(i));
         String nomAttribut = metadata.getColumnName(i);
         String append = (i==columnCount)? "":"\n #getset \n";
         templateCode = templateCode.replace("#getset", getsettemplate + append);
        templateCode = templateCode.replace("#type", typeLanguage);
        templateCode = templateCode.replace("$attribute", nomAttribut);
         listAttributes.append(typeLanguage).append(" ").append(nomAttribut).append("; \n "); 
      }
      templateCode = templateCode.replace("#attrtype", listAttributes.toString() + " \n");
      templateCode = templateCode.replace("$className", tableName.substring(0, 1).toUpperCase() + tableName.substring(1));
      fileStructure.put(tableName.substring(0, 1).toUpperCase() + tableName.substring(1), templateCode);
      return fileStructure;
   }

   public static void generateClassFiles(String directory,String extension,HashMap<String,String> aboutFiles){
      for (Map.Entry<String, String> entry : aboutFiles.entrySet()) {
         String originalKey = entry.getKey();
         String correctedKey = originalKey.substring(0, 1).toUpperCase() + originalKey.substring(1);
         String fileName = correctedKey + extension;
         String fileContent = entry.getValue();
         try {
             Files.createDirectories(Paths.get(directory));
         } catch (IOException e) {
             e.printStackTrace();
             return;
         }
 5q         Path filePath = Paths.get(directory, fileName);
         System.out.println(fileName);
         try {
             Files.write(filePath, fileContent.getBytes());
             System.out.println("File generated: " + filePath);
         } catch (IOException e) {
             e.printStackTrace();
         }
      }
   }

   public static void generate(String language, String base, String tableName,
   String templateDirectory, String pkg, String connectionString,
   String user, String pass,String targetDirectory)throws Exception{
      HashMap<String,String> h = generateFileStructure(language, base, tableName, templateDirectory, pkg, connectionString, user, pass);
      generateClassFiles(targetDirectory, getExtension(language), h);
   }



   public static void main(String[] args) throws Exception {
      generate("java", "postgresql","bdd","template.code",null,"jdbc:postgresql://localhost:5432/fw_s6", "postgres", "postgres", "./huhu");


      // Connection c = PGSQLConnection.getConnection();
      // CG.getDatabaseMetadata(c, "syntaxe", null);

      // System.out.println(CG.getDriver("postgresql"));

      // Connection c = establishConnection("postgresql", "jdbc:postgresql://localhost:5432/fw_s6", "postgres", "postgres");
      // System.out.println(c);

      // System.out.println(getVocabs("c#").get("#package"));

      // HashMap<String, String> fileStructure = generateFileStructure("c#", "postgresql", "bdd", "template.code", null,
      //  "jdbc:postgresql://localhost:5432/fw_s6", "postgres", "postgres");

      //  String generatedTemplate = fileStructure.get("GeneratedTemplate");
      //  System.out.println("Generated Template Java: \n" + generatedTemplate);

      // String directory = "D:\\S5\\Framework\\GenericDAO\\CG.java";
      // String extension = ".java";

      // HashMap<String, String> aboutFiles = new HashMap<>();
      // aboutFiles.put("dog", "package animal;\nimport java.sql.Date;\npublic class Dog {\n    String name;\n}");

      // GenerateClassFile.generateClassFiles(directory, extension, aboutFiles);
   }
}