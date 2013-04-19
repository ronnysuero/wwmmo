package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import javax.sql.rowset.serial.SerialBlob;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a wrapper around a \c PreparedStatement, for ease-of-use.
 */
public class SqlStmt implements AutoCloseable {
    private final Logger log = LoggerFactory.getLogger(SqlStmt.class);
    private static Calendar sUTC;

    static {
        sUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    private Connection mConn;
    private PreparedStatement mStmt;
    private String mSql;
    private ArrayList<Object> mParameters;
    private ArrayList<ResultSet> mResultSets;

    public SqlStmt(Connection conn, String sql, PreparedStatement stmt) {
        mConn = conn;
        mStmt = stmt;
        mSql = sql;
        mParameters = new ArrayList<Object>();
        mResultSets = new ArrayList<ResultSet>();
    }

    public void setInt(int position, int value) throws SQLException {
        mStmt.setInt(position, value);
        saveParameter(position, value);
    }
    public void setLong(int position, long value) throws SQLException {
        mStmt.setLong(position, value);
        saveParameter(position, value);
    }
    public void setDouble(int position, double value) throws SQLException {
        mStmt.setDouble(position, value);
        saveParameter(position, value);
    }
    public void setString(int position, String value) throws SQLException {
        mStmt.setString(position, value);
        saveParameter(position, value);
    }
    public void setDateTime(int position, DateTime value) throws SQLException {
        mStmt.setTimestamp(position, new Timestamp(value.getMillis()), sUTC);
        saveParameter(position, value);
    }
    public void setBlob(int position, byte[] blob) throws SQLException {
        mStmt.setBlob(position, new SerialBlob(blob));
    }
    public void setNull(int position) throws SQLException {
        mStmt.setNull(position, Types.NULL);
        saveParameter(position, "<NULL>");
    }

    private void saveParameter(int position, Object value) {
        int index = position - 1;
        while (mParameters.size() < position) {
            mParameters.add(null);
        }
        mParameters.set(index, value);
    }

    /**
     * Execute an 'update' query. That is, anything but "SELECT".
     */
    public int update() throws SQLException {
        if (log.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append(mSql);
            sb.append(System.lineSeparator());
            for (Object obj : mParameters) {
                sb.append(String.format("? = %s", obj));
                sb.append(System.lineSeparator());
            }
            log.info(sb.toString());
        }
        return mStmt.executeUpdate();
    }

    public int getAutoGeneratedID() throws SQLException {
        ResultSet rs = null;
        try {
            rs = mStmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        throw new SQLException("No auto-generated ID available.");
    }

    @SuppressWarnings("unchecked")
    public <T> T selectFirstValue(Class<T> type) throws SQLException {
        if (log.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append(mSql);
            sb.append(System.lineSeparator());
            for (Object obj : mParameters) {
                sb.append(String.format("? = %s", obj));
                sb.append(System.lineSeparator());
            }
            log.info(sb.toString());
        }

        ResultSet rs = null;
        try {
            rs = mStmt.executeQuery();
            if (rs.next()) {
                return (T) rs.getObject(1);
            }
            return null;
        } finally {
            rs.close();
        }
    }

    public ResultSet select() throws SQLException {
        if (log.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append(mSql);
            sb.append(System.lineSeparator());
            for (Object obj : mParameters) {
                sb.append(String.format("? = %s", obj));
                sb.append(System.lineSeparator());
            }
            log.info(sb.toString());
        }

        ResultSet rs = mStmt.executeQuery();
        mResultSets.add(rs);
        return rs;
    }

    @Override
    public void close() throws Exception {
        for (ResultSet rs : mResultSets) {
            rs.close();
        }
        mStmt.close();
        mConn.close();
    }
}
