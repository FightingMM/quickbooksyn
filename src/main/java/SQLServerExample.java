import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SQLServerExample {


    private static final String DB_URL = "jdbc:mysql://15.222.234.10:3306/jim?useSSL=false";
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "905I-techt";


    public static void main(String[] args) {


        try {
            // 加载 JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 建立数据库连接
            Connection connection = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD);

            // 创建一个 Statement 对象以执行 SQL 查询
            Statement statement = connection.createStatement();

            // 执行 SQL 查询
            ResultSet resultSet = statement.executeQuery("SELECT * FROM client");

            // 处理查询结果
            while (resultSet.next()) {
                System.out.println("client_no: " + resultSet.getString("client_no") );
            }

            // 关闭资源
            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
