import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import java.sql.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;



public class Launcher0412 {

    private static final String DB_URL = "jdbc:mysql://15.222.234.10:3306/jim?useSSL=false";
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "905I-techt";



    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        //Permission Mode
        Variant QBPermissionMode = new Variant(1);
        //Mode for Multi user/Single User or both, this setting is both
        Variant QBaccessMode = new Variant(2);
        //Leave Empty to use the currently opened QB File
        String fileLocation = "";  //not needed unless opening QB file which is currently not opened
        String XMLRequest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<?qbxml version=\"16.0\"?>\n" +
                "<QBXML>\n" +
                "  <QBXMLMsgsRq    onError=\"stopOnError\" >\n" +
                "    <InvoiceQueryRq   requestID=\"4\" >\n" +
                "    <TxnDateRangeFilter >  \n" +
                "    <FromTxnDate  >2024-01-31 </FromTxnDate  >  \n" +
                "    <ToTxnDate  > 2024-02-01 </ToTxnDate  > \n" +


                "    </TxnDateRangeFilter >  \n" +
                "   <IncludeLineItems >true</IncludeLineItems>  \n" +

                "    </InvoiceQueryRq >  \n" +
                "  </QBXMLMsgsRq>\n" +
                "</QBXML>";

        String appID = "";//not needed unless you want to set AppID
        String applicationName = "QB Sync Test";
        Dispatch MySessionManager = new Dispatch("QBXMLRP2.RequestProcessor");
        Dispatch.call(MySessionManager, "OpenConnection2", appID, applicationName, QBPermissionMode);
        Variant ticket = Dispatch.call(MySessionManager, "BeginSession",fileLocation, QBaccessMode);
        Variant apiResponse = Dispatch.call(MySessionManager, "ProcessRequest", ticket, XMLRequest);
        System.out.println(apiResponse.toString());
        Dispatch.call(MySessionManager, "EndSession", ticket);
        Dispatch.call(MySessionManager, "CloseConnection");


        String apiResponseString = apiResponse.toString(); // 你的 XML 字符串放在这里
        JsonArray jsonArray = convertData(apiResponseString);
        saveData(jsonArray);
        System.out.println(jsonArray);
    }


    private static void saveData(JsonArray jsonArray){

        try {
            // 加载 JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 建立数据库连接
            try (Connection connection = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD)) {
                // 遍历 JSON 数组，插入数据到 quick_invoice 表中
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    String refNumber = jsonObject.get("RefNumber").getAsString();
                    String id =generateRandomString(20);

                    // 准备 SQL 语句
                    String sql = "INSERT INTO quick_invoice (id, ref_number) VALUES (?, ?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        // 设置参数
                        preparedStatement.setString(1, id);
                        preparedStatement.setString(2, refNumber);

                        // 执行 SQL
                        preparedStatement.executeUpdate();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static JsonArray convertData(String xmlString) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlString)));

        NodeList invoiceList = document.getElementsByTagName("InvoiceRet");

        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < invoiceList.getLength(); i++) {
            Element invoiceElement = (Element) invoiceList.item(i);
            JsonObject invoiceObject = new JsonObject();

            // Example: Extracting TxnID
            String txnID = invoiceElement.getElementsByTagName("FullName").item(0).getTextContent();
            invoiceObject.addProperty("FullName", txnID);

            // Example: Extracting TimeCreated
            String timeCreated = invoiceElement.getElementsByTagName("RefNumber").item(0).getTextContent();
            invoiceObject.addProperty("RefNumber", timeCreated);

            // Add more fields as needed...

            jsonArray.add(invoiceObject);
        }

        return jsonArray;
    }

    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}









//String XMLRequest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
//        "<?qbxml version=\"13.0\"?>\n" +
//        "<QBXML>\n" +
//        "  <QBXMLMsgsRq onError=\"stopOnError\">\n" +
//        "    <BillQuery requestID=\"" + UUID.randomUUID() + "\">\n" +
//
//
//        "    </BillQuery>  \n" +
//        "  </QBXMLMsgsRq>\n" +
//        "</QBXML>";
