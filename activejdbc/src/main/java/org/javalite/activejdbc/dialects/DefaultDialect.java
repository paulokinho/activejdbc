/*
Copyright 2009-2016 Igor Polevoy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.javalite.activejdbc.dialects;

import static org.javalite.activejdbc.ModelDelegate.metaModelOf;
import static org.javalite.common.Util.blank;
import static org.javalite.common.Util.join;
import static org.javalite.common.Util.joinAndRepeat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.javalite.activejdbc.CaseInsensitiveMap;
import org.javalite.activejdbc.MetaModel;
import org.javalite.activejdbc.Registry;
import org.javalite.activejdbc.associations.Many2ManyAssociation;
import org.javalite.common.Convert;

/**
 * @author Igor Polevoy
 * @author Eric Nielsen
 */
public class DefaultDialect implements Dialect {

    protected static final Pattern ORDER_BY_PATTERN = Pattern.compile("^\\s*ORDER\\s+BY",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    protected static final Pattern GROUP_BY_PATTERN = Pattern.compile("^\\s*GROUP\\s+BY",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @Override
    public String selectStar(String table) {
        return "SELECT * FROM " + table;
    }

    @Override
    public String selectStar(String table, String where) {
        return where != null ? "SELECT * FROM " + table + " WHERE " + where : selectStar(table);
    }

    /**
     * Produces a parametrized AND query.
     * Example:
     * <pre>
     * String sql = dialect.selectStarParametrized("people", "name", "ssn", "dob");
     * //generates:
     * //SELECT * FROM people WHERE name = ? AND ssn = ? AND dob = ?
     * </pre>
     *
     *
     * @param table name of table
     * @param parameters list of parameter names
     * @return something like: "select * from table_name where name = ? and last_name = ? ..."
     */
    @Override
    public String selectStarParametrized(String table, String ... parameters) {
        StringBuilder sql = new StringBuilder().append("SELECT * FROM ").append(table).append(" WHERE ");
        join(sql, parameters, " = ? AND ");
        sql.append(" = ?");
        return sql.toString();
    }

    protected void appendEmptyRow(MetaModel metaModel, StringBuilder query) {
        query.append("DEFAULT VALUES");
    }

    protected void appendQuestions(StringBuilder query, int count) {
        joinAndRepeat(query, "?", ", ", count);
    }

    protected void appendOrderBy(StringBuilder query, List<String> orderBys) {
        if (!orderBys.isEmpty()) {
            query.append(" ORDER BY ");
            join(query, orderBys, ", ");
        }
    }

    protected void appendSubQuery(StringBuilder queryBuilder, String subQuery) {
        if (!blank(subQuery)) {
            // this is only to support findFirst("order by..."), might need to revisit later
            if (!GROUP_BY_PATTERN.matcher(subQuery).find() && !ORDER_BY_PATTERN.matcher(subQuery).find()) {
                queryBuilder.append(" WHERE");
            }
            queryBuilder.append(' ').append(subQuery);
        }
    }

    protected void appendSelect(StringBuilder queryBuilder, String tableName, String[] columns, String tableAlias, String subQuery, List<String> orderBys) {
        if (tableName == null) {
            queryBuilder.append(subQuery);
        } else {
            if (tableAlias == null) {
                String cols = columns == null? "*" : join(columns, ",");
                queryBuilder.append("SELECT ").append(cols).append(" FROM ").append(tableName);
            } else {
                queryBuilder.append("SELECT ").append(tableAlias).append(".* FROM ").append(tableName).append(' ')
                        .append(tableAlias);
            }
            appendSubQuery(queryBuilder, subQuery);
        }
        appendOrderBy(queryBuilder, orderBys);
    }

    @Override
    public String formSelect(String tableName, String[] columns, String subQuery, List<String> orderBys, long limit, long offset) {
        StringBuilder queryBuilder = new StringBuilder();
        appendSelect(queryBuilder, tableName, columns, null, subQuery, orderBys);
        return queryBuilder.toString();
    }

    @Override
    public Object overrideDriverTypeConversion(MetaModel mm, String attributeName, Object value) {
	    return value;
    }

    @Override
    public String selectCount(String from) {
        return "SELECT COUNT(*) FROM " + from;
    }

    @Override
    public String selectCount(String table, String where) {
        return "SELECT COUNT(*) FROM " + table + " WHERE " + where;
    }

    @Override
    public String selectExists(MetaModel metaModel) {
	    return "SELECT " + metaModel.getIdName() + " FROM " + metaModel.getTableName()
                + " WHERE " + metaModel.getIdName() + " = ?";
    }

    @Override
    public String selectManyToManyAssociation(Many2ManyAssociation association, String sourceFkColumnName, int questionsCount) {
        String targetTable = metaModelOf(association.getTargetClass()).getTableName();
        StringBuilder query = new StringBuilder().append("SELECT ").append(targetTable).append(".*, t.")
                .append(association.getSourceFkName()).append(" AS ").append(sourceFkColumnName).append(" FROM ")
                .append(targetTable).append(" INNER JOIN ").append(association.getJoin()).append(" t ON ")
                .append(targetTable).append('.').append(association.getTargetPk()).append(" = t.")
                .append(association.getTargetFkName()).append(" WHERE t.").append(association.getSourceFkName())
                .append(" IN (");
        appendQuestions(query, questionsCount);
        query.append(')');
        return query.toString();
    }

    @Override
    public String insertManyToManyAssociation(Many2ManyAssociation association) {
        return "INSERT INTO " + association.getJoin()
                + " (" + association.getSourceFkName() + ", " + association.getTargetFkName() + ") VALUES (?, ?)";
    }

    @Override
    public String insertParametrized(MetaModel metaModel, List<String> columns, boolean containsId) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ").append(metaModel.getTableName()).append(' ');
        if (columns.isEmpty()) {
            appendEmptyRow(metaModel, query);
        } else {
            boolean addIdGeneratorCode = (!containsId && metaModel.getIdGeneratorCode() != null);
            query.append('(');
            if (addIdGeneratorCode) {
                query.append(metaModel.getIdName()).append(", ");
            }
            join(query, columns, ", ");
            query.append(") VALUES (");
            if (addIdGeneratorCode) {
                query.append(metaModel.getIdGeneratorCode()).append(", ");
            }
            appendQuestions(query, columns.size());
            query.append(')');
        }
        return query.toString();
    }

    @Override
    public String deleteManyToManyAssociation(Many2ManyAssociation association) {
        return "DELETE FROM " + association.getJoin()
                + " WHERE " + association.getSourceFkName() + " = ? AND " + association.getTargetFkName() + " = ?";
    }

    protected void appendValue(StringBuilder query, Object value) {
        if (value == null) {
            query.append("NULL");
        } else if (value instanceof Number) {
            query.append(value);
        } else if (value instanceof java.sql.Date) {
            appendDate(query, (java.sql.Date) value);
        } else if (value instanceof java.sql.Time) {
            appendTime(query, (java.sql.Time) value);
        } else if (value instanceof java.sql.Timestamp) {
            appendTimestamp(query, (java.sql.Timestamp) value);
        } else {
            query.append('\'').append(Convert.toString(value)).append('\'');
        }
    }

    protected void appendDate(StringBuilder query, java.sql.Date value) {
        query.append("DATE ").append('\'').append(value.toString()).append('\'');
    }

    protected void appendTime(StringBuilder query, java.sql.Time value) {
        query.append("TIME ").append('\'').append(value.toString()).append('\'');
    }

    protected void appendTimestamp(StringBuilder query, java.sql.Timestamp value) {
        query.append("TIMESTAMP ").append('\'').append(value.toString()).append('\'');
    }

    @Override
    public String insert(MetaModel metaModel, Map<String, Object> attributes) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ").append(metaModel.getTableName()).append(' ');
        if (attributes.isEmpty()) {
            appendEmptyRow(metaModel, query);
        } else {
            boolean addIdGeneratorCode = (metaModel.getIdGeneratorCode() != null
                    && attributes.get(metaModel.getIdName()) == null); // do not use containsKey
            query.append('(');
            if (addIdGeneratorCode) {
                query.append(metaModel.getIdName()).append(", ");
            }
            join(query, attributes.keySet(), ", ");
            query.append(") VALUES (");
            if (addIdGeneratorCode) {
                query.append(metaModel.getIdGeneratorCode()).append(", ");
            }
            Iterator<Object> it = attributes.values().iterator();
            appendValue(query, it.next());
            while (it.hasNext()) {
                query.append(", ");
                appendValue(query, it.next());
            }
            query.append(')');
        }
        return query.toString();
    }

    @Override
    public String update(MetaModel metaModel, Map<String, Object> attributes) {
    	if (attributes.isEmpty()) {
    		throw new NoSuchElementException("No attributes set, can't create an update statement.");
    	}
        StringBuilder query = new StringBuilder().append("UPDATE ").append(metaModel.getTableName()).append(" SET ");
        String idName = metaModel.getIdName();

        // don't include the id name in the SET portion
        Map<String, Object> attributesWithoutId = new CaseInsensitiveMap<>(attributes);
        attributesWithoutId.remove(idName);

        Iterator<Entry<String, Object>> it = attributesWithoutId.entrySet().iterator();
        for (;;) {
            Entry<String, Object> attribute = it.next();
            query.append(attribute.getKey()).append(" = ");
            appendValue(query, attribute.getValue()); // Accommodates the different types
            if (it.hasNext()) {
                query.append(", ");
            } else {
                break;
            }
        }

        if (metaModel.getCompositeKeys() == null){
        	query.append(" WHERE ").append(idName).append(" = ").append(attributes.get(idName));
        } else {
        	String[] compositeKeys = metaModel.getCompositeKeys();
			for (int i = 0; i < compositeKeys.length; i++) {
				query.append(i == 0 ? " WHERE " : " AND ").append(compositeKeys[i]).append(" = ");
				appendValue(query, attributes.get(compositeKeys[i]));
			}
        }
    	return query.toString();
    }
}
