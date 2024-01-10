package glb.utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class GLBFetch {
    public static Map<String, String> from(String key, String jsonPath)throws Exception {
        try (Reader reader = new FileReader(jsonPath)) {
            Gson gson = new Gson();

            // Define the type of the map using TypeToken
            Type type = new TypeToken<Map<String, Object>>() {}.getType();

            // Convert JSON to Map
            Map<String, Object> jsonMap = gson.fromJson(reader, type);

            // Check if the value corresponding to the key is a nested map
            if (jsonMap.containsKey(key) && jsonMap.get(key) instanceof Map) {
                // If it's a nested map, return it as is
                @SuppressWarnings("unchecked")
                Map<String, String> nestedMap = (Map<String, String>) jsonMap.get(key);
                return nestedMap;
            } else if (jsonMap.containsKey(key)) {
                // If it's a direct key-value pair, create a map and return it
                Map<String, String> directMap = new HashMap<>();
                directMap.put(key, (String) jsonMap.get(key));
                return directMap;
            } else {
                throw new Exception("Key not found in the JSON file.");
            }
        } catch (IOException e) {
            throw new Exception("Error reading the JSON file: " + e.getMessage());
        }
    }
    
    public static String getTemplate(String filedirectory) throws Exception{
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filedirectory))) {
              String line;
              while ((line = br.readLine()) != null) {
                 content.append(line).append("\n"); // Append each line with a newline character
              }
        }
        return content.toString();
    }

    public static Map<String, String> ReplacePlaceholders(String language, String base, String ConnectionString, String user, String pass,
                                                     String tableName, String database) throws Exception {
    Connection con = establishConnection(base, ConnectionString, user, pass);
    ResultSetMetaData r = getDatabaseMetadata(con, tableName, database);
    Map<String, String> replacedTemplates = new HashMap<>();

    if (tableName != null && !tableName.isEmpty()) {
        String CU_template = getTemplate("D:/S5/Framework/Globa/View/templates/CU.glb");
        String RD_template = getTemplate("D:/S5/Framework/Globa/View/templates/RD.glb");
        String heads = from(language, "D:/S5/Framework/Globa/View/src/glb/settings/glb-techdata.json").get("heads");

        // Process CU template
        String startMarkerCU = "@glb-field-start";
        String endMarkerCU = "@glb-field-end";
        replacedTemplates.put("CU_" + tableName, processTemplate(language,con,"",CU_template, r, startMarkerCU, endMarkerCU, heads));

        // Process RD template
        String startMarkerRD = "@glb-occurence-start";
        String endMarkerRD = "@glb-occurence-end";
        replacedTemplates.put("RD_" + tableName, processTemplate(language,con,tableName,RD_template, r, startMarkerRD, endMarkerRD, ""));
    }
    con.close();
    return replacedTemplates;
}

private static String processTemplate(String language,Connection con,String tablename,String template, ResultSetMetaData r, String startMarker, String endMarker, String additionalMarker) throws Exception {
    int startIndex = template.indexOf(startMarker);
    int endIndex = template.indexOf(endMarker);
    Map<String, String> resultMap = from(language, "D:/S5/Framework/Globa/View/src/glb/settings/glb-techdata.json");
    String heads = resultMap.get("heads");
    if (startIndex != -1 && endIndex != -1) {
        String contentBetweenMarkers = template.substring(startIndex + startMarker.length(), endIndex);

        StringBuilder fieldContent = new StringBuilder();
        if(startMarker.equalsIgnoreCase("@glb-occurence-start")){
            ResultSet res = fetchDataFromTable(con, tablename, "");
            for (;res.next();) {
                for (int j = 1; j <= r.getColumnCount(); j++) {
                    String name = r.getColumnName(j);
                    String value = res.getString(j);
                    String replacedContent = contentBetweenMarkers.replace("@glb-key", name)
                                                                  .replace("@glb-value", value);
                    fieldContent.append(replacedContent);
                    // if(j==1)System.out.println(replacedContent.toString() + "--========-================");
                }
                // template = template.substring(0, startIndex + startMarker.length()) + fieldContent.toString() + template.substring(endIndex);
            }
            template = template.replace(contentBetweenMarkers, "");
            template = template.replace(startMarker, fieldContent);
            template = template.replace(endMarker, "");
        } else {
            for (int i = 1; i <= r.getColumnCount(); i++) {
                String name = r.getColumnName(i);
                String type = r.getColumnTypeName(i);
                String replacedContent = contentBetweenMarkers.replace("@glb-name", name)
                                                              .replace("@glb-type", type);
                fieldContent.append(replacedContent);
            }
    
            template = template.substring(0, startIndex + startMarker.length()) + fieldContent.toString() + template.substring(endIndex);
            template = template.replace(startMarker, "").replace(endMarker, "").replace(additionalMarker, "");
        }
    } else {
        System.out.println("Markers not found in the template.");
    }
     template = template.replace("@glb-heads", heads);
    return template;
}

    public static ResultSet fetchDataFromTable(Connection con, String tableName,String database)throws Exception{
        ResultSet res = con.createStatement().executeQuery("select * from "+tableName+";");
        return res;
    }

    // Method to replicate non-replaced content in templates
    public static String getContentBetweenIdenticalTags(String input,String tag) {
        String startTag = tag;
        String endTag = tag;
        int startIndex = input.indexOf(startTag);
        int endIndex = input.indexOf(endTag, startIndex + startTag.length());
    
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return input.substring(startIndex, endIndex + endTag.length());
        } else {
            return null; // Tags not found or in the incorrect order
        }
    }
    

    public static ResultSetMetaData getDatabaseMetadata(Connection connection, String tableName, String database) throws Exception {
        if (tableName != null && !tableName.isEmpty()) {
            String query = "SELECT * FROM " + tableName + " WHERE 1=0";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSet.getMetaData();
            }
        } else {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            return dbMetaData.getTables(null, null, "%", null).getMetaData();
        }
    }

    public static Connection establishConnection(String base,String ConnectionString,String user,String pass) throws Exception{
        String driver = from(base,"D:/S5/Framework/Globa/View/src/glb/settings/glb-databasedriver.json").get(base);
        try {
           Class.forName(driver);
        } catch (ClassNotFoundException e) {
              throw new RuntimeException(base+" JDBC driver not found. Make sure it's in your classpath.", e);
        }
        return DriverManager.getConnection(ConnectionString, user, pass);
    }

    public static String getDriver(String base) throws Exception{
        return from(base, "D:/S5/Framework/Globa/View/src/glb/settings/glb-databasedriver.json").get(base);
    }

    public static void main(String[] args) {
        // Example usage
        // Map<String, String> resultMap = from("jsp","D:/S5/Framework/Globa/View/src/glb/settings/glb-techdata.json");
        // System.out.println(resultMap);
        // if (resultMap != null) {
        //     System.out.println("Heads: " + resultMap.get("heads"));
        //     System.out.println("For Input: " + resultMap.get("forInput"));
        // }

        try {
            // Replace with your database connection details and other required parameters
            Map<String, String> replacedTemplates = ReplacePlaceholders("jsp", "psql", 
            "jdbc:postgresql://localhost:5432/technosyntax",
             "postgres", "postgres",
                    "language_syntax", "technosyntax");

            // Process the replaced templates as needed
            replacedTemplates.forEach((key, value) -> System.out.println(key + ": " + value));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
