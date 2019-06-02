/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;


import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mongodb.*;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import org.bson.Document;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DBUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DBUtil.class);
    private static ComboPooledDataSource comboPooledDataSource = null;
    private static ConcurrentHashMap<Long, String>map = new ConcurrentHashMap<>();
    private static ThreadLocal<Connection> transactionConnection = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            super.initialValue();
            return null;
        }
    };
    private static MongoDatabase database;

    public static boolean IsEmbedDB = false;
    public static boolean UseMongoDB = false;

        public static void init(IConfig config) {
            String embedDB = config.getProperty(BrokerConstants.EMBED_DB_PROPERTY_NAME);
            if (embedDB != null && embedDB.equals("1")) {
                IsEmbedDB = true;
                LOG.info("Use h2 database");
            } else if (embedDB != null && embedDB.equals("0")) {
                IsEmbedDB = false;
                LOG.info("Use mysql database");
            } else if(embedDB != null && embedDB.equals("2")) {
                IsEmbedDB = false;
                UseMongoDB = true;
                LOG.info("Use mysql + mongodb database");
            } else {
                IsEmbedDB = true;
                LOG.info("Invalid db config. Use h2 database");
            }

            if (comboPooledDataSource == null) {
                String migrateLocation;
                if (IsEmbedDB) {
                    migrateLocation = "filesystem:./migrate/h2";
                    comboPooledDataSource = new ComboPooledDataSource();

                    comboPooledDataSource.setJdbcUrl( "jdbc:h2:./h2db/wfchat;AUTO_SERVER=TRUE;MODE=MySQL" );
                    comboPooledDataSource.setUser("SA");
                    comboPooledDataSource.setPassword("SA");
                    comboPooledDataSource.setMinPoolSize(5);
                    comboPooledDataSource.setAcquireIncrement(5);
                    comboPooledDataSource.setMaxPoolSize(20);

                    comboPooledDataSource.setIdleConnectionTestPeriod(60 * 5);
                    comboPooledDataSource.setMinPoolSize(3);
                    comboPooledDataSource.setInitialPoolSize(3);

                    try {
                        comboPooledDataSource.setDriverClass( "org.h2.Driver" ); //loads the jdbc driver
                    } catch (PropertyVetoException e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e);
                        System.exit(-1);
                    }
                } else {
                    migrateLocation = "filesystem:./migrate/mysql";
                    comboPooledDataSource = new ComboPooledDataSource("mysql");
                    try {
                        String url01 = comboPooledDataSource.getJdbcUrl().substring(0,comboPooledDataSource.getJdbcUrl().indexOf("?"));

                        String url02 = url01.substring(0,url01.lastIndexOf("/"));

                        String datasourceName = url01.substring(url01.lastIndexOf("/")+1);
                        // 连接已经存在的数据库，如：mysql
                        Connection connection = DriverManager.getConnection(url02, comboPooledDataSource.getUser(), comboPooledDataSource.getPassword());
                        Statement statement = connection.createStatement();

                        // 创建数据库
                        statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + datasourceName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");

                        statement.close();
                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                }
                Flyway flyway = Flyway.configure().dataSource(comboPooledDataSource).locations(migrateLocation).baselineOnMigrate(true).load();
                flyway.migrate();

                if (UseMongoDB) {
                    initMongoDB(config);
                }
            }
        }

        private static void initMongoDB(IConfig config) {
            String mongoClientUri = config.getProperty(BrokerConstants.MONGODB_Client_URI);

            MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoClientUri));

            String mongoDBName = config.getProperty(BrokerConstants.MONGODB_Database);
            database = mongoClient.getDatabase(mongoDBName);

            MongoCollection<Document> collectionMessage =  database.getCollection("t_messages_0");
            ListIndexesIterable<Document> indexs = collectionMessage.listIndexes();

            ArrayList<String> indexNames = new ArrayList<>();
            indexs.forEach(new Consumer<Document>() {
                @Override
                public void accept(Document document) {
                    indexNames.add(document.getString("name"));
                }
            });

            if (indexNames.size() < 2) {
                for (int i = 0; i < 128; i++) {
                    String userMessagsName = "t_user_messages_" + i;

                    MongoCollection<Document> collection = database.getCollection(userMessagsName);

                    BasicDBObject createIndex = new BasicDBObject();
                    createIndex.put("_uid", 1);
                    createIndex.put("_seq", -1);
                    IndexOptions options = new IndexOptions();
                    options.background(true);
                    options.expireAfter(6L, TimeUnit.MINUTES);
                    collection.createIndex(createIndex, options);
                }

                for (int i = 0; i < 36; i++) {
                    String messagsName = "t_messages_" + i;

                    MongoCollection<Document> collection = database.getCollection(messagsName);

                    BasicDBObject createIndex1 = new BasicDBObject();
                    createIndex1.put("_type", 1);
                    createIndex1.put("_target", 1);
                    createIndex1.put("_line", 1);
                    IndexOptions options1 = new IndexOptions();
                    options1.background(true);
                    collection.createIndex(createIndex1, options1);


                    BasicDBObject createIndex2 = new BasicDBObject();
                    createIndex2.put("_dt", -1);
                    IndexOptions options2 = new IndexOptions();
                    options2.background(true);
                    options2.expireAfter(6L, TimeUnit.MINUTES);
                    collection.createIndex(createIndex2, options2);


                    BasicDBObject createIndex3 = new BasicDBObject();
                    createIndex3.put("_from", 1);
                    collection.createIndex(createIndex3, options1);
                }
            }

        }

        private static List<String> getCreateSql() {
            List<String> out = new ArrayList<>();
            try{
                BufferedReader br = new BufferedReader(new FileReader("h2/create_table.sql"));//构造一个BufferedReader类来读取文件
                String s = null;
                StringBuilder result = new StringBuilder();
                while((s = br.readLine())!=null) {
                    result.append(s);
                    if (s.contains(";")) {
                        out.add(result.toString());
                        result = new StringBuilder();
                    }
                }
                br.close();
            }catch(Exception e){
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
            return out;
        }
        //从数据源中获取数据库的连接
        public static Connection getConnection() throws SQLException {
            long threadId = Thread.currentThread().getId();

            if (map.get(threadId) != null) {
                LOG.error("error here!!!! DB connection not close correctly");
            }
            map.put(threadId, Thread.currentThread().getStackTrace().toString());
            Connection connection = transactionConnection.get();
            if (connection != null) {
                LOG.debug("Thread {} get db connection {}", threadId, connection);
                return connection;
            }

            connection = comboPooledDataSource.getConnection();
            LOG.debug("Thread {} get db connection {}", threadId, connection);
            return connection;
        }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public static void setDatabase(MongoDatabase database) {
        DBUtil.database = database;
    }

    public static void beginTransaction() {
            try {
                Connection connection = getConnection();
                connection.setAutoCommit(false);
                transactionConnection.set(connection);
            } catch (SQLException e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        }

    public static void commit() {
        try {
            Connection connection = transactionConnection.get();
            if (connection != null) {
                connection.commit();
                connection.setAutoCommit(true);
                transactionConnection.remove();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public static void roolback() {
        try {
            Connection connection = transactionConnection.get();
            if (connection != null) {
                connection.rollback();
                connection.setAutoCommit(true);
                transactionConnection.remove();
            };
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

        //释放资源，将数据库连接还给数据库连接池
        public static void closeDB(Connection conn,PreparedStatement ps,ResultSet rs) {
            LOG.debug("Thread {} release db connection {}", Thread.currentThread().getId(), conn);
            try {
                if (rs!=null) {
                    rs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }

            try {
                if (ps!=null) {
                    ps.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }

            try {
                if (conn!=null && transactionConnection.get() != conn) {
                    conn.close();
                    map.remove(Thread.currentThread().getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        }
        //释放资源，将数据库连接还给数据库连接池
        public static void closeDB(Connection conn, PreparedStatement ps) {
            LOG.debug("Thread {} release db connection {}", Thread.currentThread().getId(), conn);
             try {
                if (ps!=null) {
                    ps.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                 Utility.printExecption(LOG, e);
            }

            try {
                if (conn!=null && transactionConnection.get() != conn) {
                    conn.close();
                    map.remove(Thread.currentThread().getId());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        }
}
