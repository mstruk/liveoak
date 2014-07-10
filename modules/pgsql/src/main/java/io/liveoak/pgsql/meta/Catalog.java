package io.liveoak.pgsql.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class Catalog {

    private Map<TableRef, Table> tables;

    public Catalog(Map<TableRef, Table> tables) {
        Map<TableRef, Table> tablesWithIds = new HashMap<>();

        // group by name (same name in multiple schemas)
        LinkedHashMap<String, List<TableRef>> seenNames = new LinkedHashMap<>();
        for (TableRef ref: tables.keySet()) {
            List<TableRef> fullNames = seenNames.get(ref.name());
            if (fullNames == null) {
                fullNames = new LinkedList<>();
                seenNames.put(ref.name(), fullNames);
            }
            fullNames.add(ref);
        }

        // set ids as either short (name) or long (schema.name)
        for (Map.Entry<String, List<TableRef>> e: seenNames.entrySet()) {
            if (e.getValue().size() > 1) {
                for (TableRef ref: e.getValue()) {
                    tablesWithIds.put(ref, new Table(ref.asUnquotedIdentifier(), tables.get(ref)));
                }
            } else {
                TableRef ref = e.getValue().get(0);
                tablesWithIds.put(ref, new Table(e.getKey(), tables.get(ref)));
            }
        }

        this.tables = Collections.unmodifiableMap(tablesWithIds);
    }

    public Table table(TableRef tableRef) {
        if (tableRef.schema() != null) {
            return tables.get(tableRef);
        } else {
            for (Table t: tables.values()) {
                if (t.name().equals(tableRef.name())) {
                    return t;
                }
            }
        }
        return null;
    }

    public List<String> tableIds() {
        List<String> ret = new LinkedList<>();
        for (Table t: tables.values()) {
            ret.add(t.id());
        }
        return ret;
    }

    public Table tableById(String id) {
        return table(new TableRef(id));
    }
}
