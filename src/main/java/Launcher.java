import com.google.gson.Gson;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import java.sql.*;
import java.util.Date;
import java.util.Random;


public class Launcher {

    private static final String DB_URL = "jdbc:mysql://35.183.171.185:3306/easyerp?useSSL=false";
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "905I-techt";


    public static void main(String[] args) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    // 获取当前时间
                    Date date = new Date();
                    // 设置日期格式
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    System.out.println("Schedule start:"+ sdf.format(date));
                    fetchDataAndSave();
                    fetchDataAndSaveModify();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        // 每隔5秒执行一次
        long delay = 0; // 延迟0秒后开始执行
        long period = 500000; // 每500秒执行一次

        // 使用定时器调度任务
        timer.scheduleAtFixedRate(task, delay, period);


    }


    public static void fetchDataAndSave() throws ParserConfigurationException, IOException, SAXException {
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
                "    <InvoiceQueryRq   requestID=\"" + UUID.randomUUID() + "\" >\n" +
                "    <TxnDateRangeFilter >  \n" +
                "    <FromTxnDate  >" + currentTime() + " </FromTxnDate  >  \n" +
                "    <ToTxnDate  > " + currentTimePlusOneDay() + " </ToTxnDate  > \n" +
                "    </TxnDateRangeFilter >  \n" +
                "   <IncludeLineItems >true</IncludeLineItems>  \n" +

                "    </InvoiceQueryRq >  \n" +
                "  </QBXMLMsgsRq>\n" +
                "</QBXML>";

        String appID = "";//not needed unless you want to set AppID
        String applicationName = "QB Sync Test";
        Dispatch MySessionManager = new Dispatch("QBXMLRP2.RequestProcessor");
        Dispatch.call(MySessionManager, "OpenConnection2", appID, applicationName, QBPermissionMode);
        Variant ticket = Dispatch.call(MySessionManager, "BeginSession", fileLocation, QBaccessMode);
        Variant apiResponse = Dispatch.call(MySessionManager, "ProcessRequest", ticket, XMLRequest);
        Dispatch.call(MySessionManager, "EndSession", ticket);
        Dispatch.call(MySessionManager, "CloseConnection");


        String apiResponseString = apiResponse.toString(); // 你的 XML 字符串放在这里
        JsonArray jsonArray = convertData(apiResponseString);
        saveData(jsonArray);
        //System.out.println(jsonArray);
    }

    public static void fetchDataAndSaveModify() throws ParserConfigurationException, IOException, SAXException {
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
                "    <InvoiceQueryRq   requestID=\"" + UUID.randomUUID() + "\" >\n" +
                "    <ModifiedDateRangeFilter >  \n" +
                "    <FromModifiedDate   >" + currentTime() + " </FromModifiedDate   >  \n" +
                "    <ToModifiedDate   > " + currentTimePlusOneDay() + " </ToModifiedDate   > \n" +
                "    </ModifiedDateRangeFilter >  \n" +

                "   <IncludeLineItems >true</IncludeLineItems>  \n" +

                "    </InvoiceQueryRq >  \n" +
                "  </QBXMLMsgsRq>\n" +
                "</QBXML>";

        String appID = "";//not needed unless you want to set AppID
        String applicationName = "QB Sync Test";
        Dispatch MySessionManager = new Dispatch("QBXMLRP2.RequestProcessor");
        Dispatch.call(MySessionManager, "OpenConnection2", appID, applicationName, QBPermissionMode);
        Variant ticket = Dispatch.call(MySessionManager, "BeginSession", fileLocation, QBaccessMode);
        Variant apiResponse = Dispatch.call(MySessionManager, "ProcessRequest", ticket, XMLRequest);
        Dispatch.call(MySessionManager, "EndSession", ticket);
        Dispatch.call(MySessionManager, "CloseConnection");


        String apiResponseString = apiResponse.toString(); // 你的 XML 字符串放在这里
        JsonArray jsonArray = convertData(apiResponseString);
        modifyData(jsonArray);
        //System.out.println(jsonArray);
    }


    private static void modifyData(JsonArray jsonArray) {

        try {
            // 加载 JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            ArrayList<String> modifyDataList = new ArrayList<>();
            try (Connection connection = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD)) {
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    String refNumber = jsonObject.get("RefNumber").getAsString();
                    String checkSql = "SELECT id,time_modified FROM quick_invoice WHERE ref_number = ?";
                    try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                        checkStatement.setString(1, refNumber);
                        ResultSet resultSet = checkStatement.executeQuery();
                        if (resultSet.next()) {
                            String TimeModified = jsonObject.get("TimeModified").getAsString();
                            Timestamp timeModified = parseTimestamp(TimeModified);
                            String formattedJsonObjectTimeModified = timeModified.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                            Timestamp resultSetTimeModified = resultSet.getTimestamp("time_modified");
                            String formattedTimeModified = resultSetTimeModified.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                            if (!formattedTimeModified.equals(formattedJsonObjectTimeModified)) {
                                System.out.println("Data modify : " + refNumber);
                                String invoiceId = resultSet.getString("id");
                                // 调用方法修改 quick_invoice 表中的字段值
                                updateInvoice(connection, invoiceId, jsonObject);
                                insertInvoiceItems(invoiceId, connection, jsonObject);
                                modifyDataList.add(refNumber);
                            }
                        } else {
                            System.out.println("Invoice not found for ref_number: " + refNumber);
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 更新 Invoice 数据
    private static void updateInvoice(Connection connection, String invoiceId, JsonObject jsonObject) throws SQLException {
        // 解析 jsonObject 中的数据并更新到数据库中
        // 例如：
        String isPaid = jsonObject.get("IsPaid").getAsString();
        Timestamp timeModified = parseTimestamp(jsonObject.get("TimeModified").getAsString());
        Timestamp txnDate = Timestamp.valueOf(jsonObject.get("TxnDate").getAsString() + " 00:00:00");
        String isPending = jsonObject.get("IsPending").getAsString();
        Timestamp dueDate = Timestamp.valueOf(jsonObject.get("DueDate").getAsString() + " 00:00:00");
        Timestamp shipDate = Timestamp.valueOf(jsonObject.get("ShipDate").getAsString() + " 00:00:00");
        double subtotal = jsonObject.get("Subtotal").getAsDouble();
        double salesTaxPercentage = jsonObject.get("SalesTaxPercentage").getAsDouble();
        double salesTaxTotal = jsonObject.get("SalesTaxTotal").getAsDouble();
        double appliedAmount = jsonObject.get("AppliedAmount").getAsDouble();
        double balanceRemaining = jsonObject.get("BalanceRemaining").getAsDouble();
        Timestamp updateTime = new Timestamp(System.currentTimeMillis());
        String updateSql = "UPDATE quick_invoice SET is_paid = ?, time_modified = ?, txn_date = ?, is_pending = ?, due_date = ?, ship_date = ?, subtotal = ?, sales_tax_percentage = ?, sales_tax_total = ?, applied_amount = ?, balance_remaining = ?, update_time = ? WHERE id = ?";

        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
            updateStatement.setString(1, isPaid);
            updateStatement.setTimestamp(2, timeModified);
            updateStatement.setTimestamp(3, txnDate);
            updateStatement.setString(4, isPending);
            updateStatement.setTimestamp(5, dueDate);
            updateStatement.setTimestamp(6, shipDate);
            updateStatement.setDouble(7, subtotal);
            updateStatement.setDouble(8, salesTaxPercentage);
            updateStatement.setDouble(9, salesTaxTotal);
            updateStatement.setDouble(10, appliedAmount);
            updateStatement.setDouble(11, balanceRemaining);
            updateStatement.setTimestamp(12, updateTime);
            updateStatement.setString(13, invoiceId);
            updateStatement.executeUpdate();
        }


        String updateItemSql = "UPDATE quick_invoice_item SET is_delete = 1, update_time = ? WHERE invoice_id = ?";
        Timestamp updateItemTime = new Timestamp(System.currentTimeMillis());

        try (PreparedStatement updateStatement = connection.prepareStatement(updateItemSql)) {
            updateStatement.setTimestamp(1, updateItemTime);
            updateStatement.setString(2, invoiceId);
            updateStatement.executeUpdate();
        }

    }


    private static void saveData(JsonArray jsonArray) {

        try {
            // 加载 JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection connection = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD)) {
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    String refNumber = jsonObject.get("RefNumber").getAsString();
                    String checkSql = "SELECT COUNT(*) AS count FROM quick_invoice WHERE ref_number = ?";
                    try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                        checkStatement.setString(1, refNumber);
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        int count = resultSet.getInt("count");
                        if (count == 0) {
                            System.out.println("Data inserted : " + refNumber);
                            String invoiceId = insertInvoice(connection, jsonObject);
                            insertInvoiceItems(invoiceId, connection, jsonObject);
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String insertInvoice(Connection connection, JsonObject jsonObject) throws SQLException {
        String id = generateRandomString(20);
        String txnID = jsonObject.get("TxnID").getAsString();
        Timestamp timeCreated = parseTimestamp(jsonObject.get("TimeCreated").getAsString());
        Timestamp timeModified = parseTimestamp(jsonObject.get("TimeModified").getAsString());
        String txnNumber = jsonObject.get("TxnNumber").getAsString();
        String customerRefFullName = jsonObject.get("CustomerRef_FullName").getAsString();
        Timestamp txnDate = Timestamp.valueOf(jsonObject.get("TxnDate").getAsString() + " 00:00:00");
        String refNumber = jsonObject.get("RefNumber").getAsString();
        String billAddress = jsonObject.get("BillAddress").getAsString();
        String isPending = jsonObject.get("IsPending").getAsString();
        String isFinanceCharge = jsonObject.get("IsFinanceCharge").getAsString();
        Timestamp dueDate = Timestamp.valueOf(jsonObject.get("DueDate").getAsString() + " 00:00:00");
        Timestamp shipDate = Timestamp.valueOf(jsonObject.get("ShipDate").getAsString() + " 00:00:00");
        double subtotal = jsonObject.get("Subtotal").getAsDouble();
        double salesTaxPercentage = jsonObject.get("SalesTaxPercentage").getAsDouble();
        double salesTaxTotal = jsonObject.get("SalesTaxTotal").getAsDouble();
        double appliedAmount = jsonObject.get("AppliedAmount").getAsDouble();
        double balanceRemaining = jsonObject.get("BalanceRemaining").getAsDouble();
        String isPaid = jsonObject.get("IsPaid").getAsString();
        Timestamp createTime = new Timestamp(System.currentTimeMillis());
        int isDelete = 0;
        String sql = "INSERT INTO quick_invoice (id, txn_id, time_created, time_modified, txn_number, customer_ref_full_name, txn_date, ref_number, bill_address, is_pending, is_finance_charge, due_date, ship_date, subtotal, sales_tax_percentage, sales_tax_total, applied_amount, balance_remaining, is_paid, create_time, is_delete) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, txnID);
            preparedStatement.setTimestamp(3, timeCreated);
            preparedStatement.setTimestamp(4, timeModified);
            preparedStatement.setString(5, txnNumber);
            preparedStatement.setString(6, customerRefFullName);
            preparedStatement.setTimestamp(7, txnDate);
            preparedStatement.setString(8, refNumber);
            preparedStatement.setString(9, billAddress);
            preparedStatement.setString(10, isPending);
            preparedStatement.setString(11, isFinanceCharge);
            preparedStatement.setTimestamp(12, dueDate);
            preparedStatement.setTimestamp(13, shipDate);
            preparedStatement.setDouble(14, subtotal);
            preparedStatement.setDouble(15, salesTaxPercentage);
            preparedStatement.setDouble(16, salesTaxTotal);
            preparedStatement.setDouble(17, appliedAmount);
            preparedStatement.setDouble(18, balanceRemaining);
            preparedStatement.setString(19, isPaid);
            preparedStatement.setTimestamp(20, createTime);
            preparedStatement.setInt(21, isDelete);

            preparedStatement.executeUpdate();
        }
        return id;
    }

    // 插入 Invoice Line 数据
    private static void insertInvoiceItems(String invoiceId, Connection connection, JsonObject jsonObject) throws SQLException {
        JsonArray invoiceLineRetArray = jsonObject.getAsJsonArray("InvoiceLineRet");

        for (JsonElement element : invoiceLineRetArray) {
            JsonObject invoiceLineRetObject = element.getAsJsonObject();
            String id = generateRandomString(20);
            String fullName = invoiceLineRetObject.get("FullName").getAsString();
            String Desc = invoiceLineRetObject.get("Desc").getAsString();
            double quantity = invoiceLineRetObject.get("Quantity").getAsDouble();
            double rate = invoiceLineRetObject.get("Rate").getAsDouble();
            double amount = invoiceLineRetObject.get("Amount").getAsDouble();
            Timestamp createTime = new Timestamp(System.currentTimeMillis());

            String sql = "INSERT INTO quick_invoice_item (id, invoice_id, full_name, quantity, rate, amount, create_time, is_delete ,description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, invoiceId);
                preparedStatement.setString(3, fullName);
                preparedStatement.setDouble(4, quantity);
                preparedStatement.setDouble(5, rate);
                preparedStatement.setDouble(6, amount);
                preparedStatement.setTimestamp(7, createTime);
                preparedStatement.setInt(8, 0);
                preparedStatement.setString(9, Desc);
                preparedStatement.executeUpdate();
            }
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

            // TxnID
            String txnID = invoiceElement.getElementsByTagName("TxnID").item(0).getTextContent();
            invoiceObject.addProperty("TxnID", txnID);

            // TimeCreated
            String timeCreated = invoiceElement.getElementsByTagName("TimeCreated").item(0).getTextContent();
            invoiceObject.addProperty("TimeCreated", timeCreated);

            // TimeModified
            String timeModified = invoiceElement.getElementsByTagName("TimeModified").item(0).getTextContent();
            invoiceObject.addProperty("TimeModified", timeModified);

            // TxnNumber
            String txnNumber = invoiceElement.getElementsByTagName("TxnNumber").item(0).getTextContent();
            invoiceObject.addProperty("TxnNumber", txnNumber);

            // CustomerRef - FullName
            String customerRefFullName = invoiceElement.getElementsByTagName("CustomerRef").item(0).getTextContent();
            String[] customerRefFullNameparts = customerRefFullName.split("\\n");
            invoiceObject.addProperty("CustomerRef_FullName", customerRefFullNameparts[2]);

            // TxnDate
            String txnDate = invoiceElement.getElementsByTagName("TxnDate").item(0).getTextContent();
            invoiceObject.addProperty("TxnDate", txnDate);

            // RefNumber
            String refNumber = invoiceElement.getElementsByTagName("RefNumber").item(0).getTextContent();
            invoiceObject.addProperty("RefNumber", refNumber);

            // BillAddress
            String billAddress = invoiceElement.getElementsByTagName("BillAddress").item(0).getTextContent();

            String[] parts = billAddress.split("\\n");

            // 创建一个新的JsonObject
            JsonObject addressJson = new JsonObject();

            // 添加地址信息到JsonObject中
            if (parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) {
                addressJson.addProperty("Company", parts[1]);
            }

            if (parts.length > 3 && parts[3] != null && !parts[3].isEmpty()) {
                addressJson.addProperty("Address", parts[3]);
            }

            if (parts.length > 4 && parts[4] != null && !parts[4].isEmpty()) {
                addressJson.addProperty("City", parts[4]);
            }

            if (parts.length > 5 && parts[5] != null && !parts[5].isEmpty()) {
                addressJson.addProperty("Province", parts[5]);
            }

            if (parts.length > 7 && parts[7] != null && !parts[7].isEmpty()) {
                addressJson.addProperty("PostalCode", parts[7]);
            }
            invoiceObject.addProperty("BillAddress", addressJson.toString());


            // IsPending
            String isPending = invoiceElement.getElementsByTagName("IsPending").item(0).getTextContent();
            invoiceObject.addProperty("IsPending", isPending);

            // IsFinanceCharge
            String isFinanceCharge = invoiceElement.getElementsByTagName("IsFinanceCharge").item(0).getTextContent();
            invoiceObject.addProperty("IsFinanceCharge", isFinanceCharge);

            // DueDate
            String dueDate = invoiceElement.getElementsByTagName("DueDate").item(0).getTextContent();
            invoiceObject.addProperty("DueDate", dueDate);

            // ShipDate
            String shipDate = invoiceElement.getElementsByTagName("ShipDate").item(0).getTextContent();
            invoiceObject.addProperty("ShipDate", shipDate);

            // Subtotal
            String subtotal = invoiceElement.getElementsByTagName("Subtotal").item(0).getTextContent();
            invoiceObject.addProperty("Subtotal", subtotal);

            // SalesTaxPercentage
            String salesTaxPercentage = invoiceElement.getElementsByTagName("SalesTaxPercentage").item(0).getTextContent();
            invoiceObject.addProperty("SalesTaxPercentage", salesTaxPercentage);

            // SalesTaxTotal
            String salesTaxTotal = invoiceElement.getElementsByTagName("SalesTaxTotal").item(0).getTextContent();
            invoiceObject.addProperty("SalesTaxTotal", salesTaxTotal);

            // AppliedAmount
            String appliedAmount = invoiceElement.getElementsByTagName("AppliedAmount").item(0).getTextContent();
            invoiceObject.addProperty("AppliedAmount", appliedAmount);

            // BalanceRemaining
            String balanceRemaining = invoiceElement.getElementsByTagName("BalanceRemaining").item(0).getTextContent();
            invoiceObject.addProperty("BalanceRemaining", balanceRemaining);

            // IsPaid
            String isPaid = invoiceElement.getElementsByTagName("IsPaid").item(0).getTextContent();
            invoiceObject.addProperty("IsPaid", isPaid);

            // InvoiceLineRet - ItemRef - FullName
            NodeList invoiceLineRetList = invoiceElement.getElementsByTagName("InvoiceLineRet");
            JsonArray invoiceLineArray = new JsonArray();
            for (int j = 0; j < invoiceLineRetList.getLength(); j++) {

                Element invoiceLineElement = (Element) invoiceLineRetList.item(j);
                NodeList itemRefList = invoiceLineElement.getElementsByTagName("ItemRef");
                if (invoiceLineElement.getElementsByTagName("ItemRef").item(0) != null) {
                    JsonObject invoiceLineObject = new JsonObject();
                    for (int k = 0; k < itemRefList.getLength(); k++) {
                        Element itemRefElement = (Element) itemRefList.item(k);
                        String fullName = itemRefElement.getElementsByTagName("FullName").item(0).getTextContent();
                        invoiceLineObject.addProperty("FullName", fullName);
                    }

                    String Quantity = invoiceLineElement.getElementsByTagName("Quantity").item(0).getTextContent();
                    invoiceLineObject.addProperty("Quantity", Quantity);
                    String Rate = invoiceLineElement.getElementsByTagName("Rate").item(0).getTextContent();
                    invoiceLineObject.addProperty("Rate", Rate);
                    String Amount = invoiceLineElement.getElementsByTagName("Amount").item(0).getTextContent();
                    invoiceLineObject.addProperty("Amount", Amount);
                    String Desc = invoiceLineElement.getElementsByTagName("Desc").item(0).getTextContent();
                    invoiceLineObject.addProperty("Desc", Desc);
                    invoiceLineArray.add(invoiceLineObject);
                }


            }
            invoiceObject.add("InvoiceLineRet", invoiceLineArray);

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

    public static Timestamp parseTimestamp(String timestampString) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            Date parsedDate = dateFormat.parse(timestampString);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String currentTime() {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();

        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 格式化日期时间
        String formattedTime = currentTime.format(formatter);
        return formattedTime;
    }

    public static String currentTimePlusOneDay() {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();

        // 加一天
        LocalDateTime tomorrow = currentTime.plusDays(1);

        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 格式化日期时间
        String formattedTime = tomorrow.format(formatter);
        return formattedTime;
    }

}


