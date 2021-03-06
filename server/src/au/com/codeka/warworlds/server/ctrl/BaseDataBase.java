package au.com.codeka.warworlds.server.ctrl;

import java.sql.SQLException;
import java.util.Collection;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatConversationParticipant;

/**
 * This is our base "database" class that includes some common methods for working with the database.
 */
public class BaseDataBase {
    private Transaction mTransaction;

    public BaseDataBase() {
    }
    public BaseDataBase(Transaction trans) {
        mTransaction = trans;
    }

    public Transaction getTransaction() {
        return mTransaction;
    }

    protected SqlStmt prepare(String sql) throws SQLException {
        if (mTransaction != null) {
            return mTransaction.prepare(sql);
        } else {
            return DB.prepare(sql);
        }
    }

    protected SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        if (mTransaction != null) {
            return mTransaction.prepare(sql, autoGenerateKeys);
        } else {
            return DB.prepare(sql, autoGenerateKeys);
        }
    }

    protected static String buildInClause(ChatConversationParticipant[] participants) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < participants.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(participants[i].getEmpireID());
        }
        sb.append(")");
        return sb.toString();
    }

    protected static String buildInClause(int[] ids) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ids[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    protected static String buildInClause(Integer[] ids) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ids[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    protected static String buildInClause(Collection<Integer> ids) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(id);
        }
        sb.append(")");
        return sb.toString();
    }
}
