package glb.utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;

public class GLBFetch {
    public static Map<String, String> from(String key,String jsonPath) {
        jsonPath = "glb/settings/glb-techdata.json";

        try (Reader reader = new FileReader(jsonPath)) {
            Gson gson = new Gson();

            // Define the type of the map using TypeToken
            Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();

            // Convert JSON to Map
            Map<String, Map<String, String>> jsonMap = gson.fromJson(reader, type);

            // Retrieve the corresponding value based on the provided key
            if (jsonMap.containsKey(key)) {
                return jsonMap.get(key);
            } else {
                System.err.println("Key not found in the JSON file.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
            return null;8
        }
    }

    // public static ResultSetMetaData getDatabaseMetadata(Connection connection,String tableName, String database) throws Exception {
    //     String query = "SELECT * FROM " + tableName + " WHERE 1=0";
    //     try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
    //         ResultSet resultSet = preparedStatement.executeQuery();
    //         ResultSetMetaData metaData = resultSet.getMetaData();
    //         return metaData;
    //     }
    // }

    // public static Connection establishConnection(String base,String ConnectionString,String user,String pass) throws Exception{
    //     String driver = from(base,"glb/settings/glb-techdata.json").get("");
    //     try {
    //        Class.forName(driver);
    //     } catch (ClassNotFoundException e) {
    //           throw new RuntimeException(base+" JDBC driver not found. Make sure it's in your classpath.", e);
    //     }
    //     return DriverManager.getConnection(ConnectionString, user, pass);
    // }

    public static void main(String[] args) {
        // Example usage
        Map<String, String> resultMap = from("jsp","");
        System.out.println(resultMap);
        if (resultMap != null) {
            System.out.println("Heads: " + resultMap.get("heads"));
            System.out.println("For Input: " + resultMap.get("forInput"));
        }
    }
}
