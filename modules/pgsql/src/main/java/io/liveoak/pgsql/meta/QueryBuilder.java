package io.liveoak.pgsql.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class QueryBuilder {

    private Catalog catalog;

    public QueryBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    public String selectAllFromTable(String table) {
        String tableId = new TableRef(table).asQuotedIdentifier();
        return "SELECT * FROM " + tableId;
    }

    public PreparedStatement prepareSelectAllFromTable(Connection con, String table) throws SQLException {
        return con.prepareStatement(selectAllFromTable(table));
    }

    public PreparedStatement prepareSelectFromTableWhereId(Connection con, String table, String id) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(selectAllFromTable(table) + " WHERE ");

        Table tableDef = catalog.table(new TableRef(table));
        if (tableDef == null) {
            throw new IllegalStateException("No such table: " + table);
        }
        int i = 0;

        String [] vals = PrimaryKey.splitId(id);
        List<Column> cols = tableDef.pk().columns();

        if (cols.size() != vals.length) {
            throw new IllegalStateException("Id is incompatible with table definition: " + table + " contains "
                    + cols.size() + " columns, while id " + id + " contains " + vals.length + " components");
        }

        for (Column c: cols) {
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(c.quotedName()).append("=?");
            i++;
        }


        PreparedStatement ps = con.prepareStatement(sb.toString());
        i = 1;
        for (String v: vals) {
            ps.setString(i, v);
            i++;
        }

        return ps;
    }
}
