package sqlancer.tidb.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.schema.TableIndex;
import sqlancer.tidb.TiDBSchema.TiDBTable;
import sqlancer.tidb.ast.TiDBSelect;
import sqlancer.tidb.ast.TiDBText;

public class TiDBHintGenerator {
	
	private TiDBSelect select;
	private List<TiDBTable> tables;
	private final StringBuilder sb = new StringBuilder();

	enum IndexHint {
		MERGE_JOIN, //
		INL_JOIN, //
		INL_HASH_JOIN, //
		INL_MERGE_JOIN, //
		HASH_JOIN, //
		HASH_AGG, //
		STREAM_AGG, //
		USE_INDEX, //
		IGNORE_INDEX, //
		AGG_TO_COP, //
		// READ_FROM_STORAGE
		USE_INDEX_MERGE, //
		NO_INDEX_MERGE, //
		USE_TOJA;
	}

	public TiDBHintGenerator(TiDBSelect select, List<TiDBTable> tables) {
		this.select = select;
		this.tables = tables;
	}

	public static void generateHints(TiDBSelect select, List<TiDBTable> tables) {
		new TiDBHintGenerator(select, tables).generate();
		
	}

	private void generate() {
		TiDBTable table = Randomly.fromList(tables);
		switch (Randomly.fromOptions(IndexHint.values())) {
		case MERGE_JOIN:
			tablesHint("MERGE_JOIN");
			break;
		case INL_JOIN:
			tablesHint("INL_JOIN");
			break;
		case INL_HASH_JOIN:
			tablesHint("INL_HASH_JOIN");
			break;
		case INL_MERGE_JOIN:
			tablesHint("INL_MERGE_JOIN");
			break;
		case HASH_JOIN:
			tablesHint("HASH_JOIN");
			break;
		case HASH_AGG:
			sb.append("HASH_AGG()");
			break;
		case STREAM_AGG:
			sb.append("STREAM_AGG()");
			break;
		case USE_INDEX:
			indexesHint("USE_INDEX");
			break;
		case IGNORE_INDEX:
			indexesHint("IGNORE_INDEX");
			break;
		case AGG_TO_COP:
			sb.append("AGG_TO_COP()");
			break;
		case USE_INDEX_MERGE:
			if (true) {
				// https://github.com/pingcap/tidb/issues/15994
				// https://github.com/pingcap/tidb/issues/15992
				// https://github.com/pingcap/tidb/issues/15991
				throw new IgnoreMeException();
			}
			if (table.hasIndexes()) {
				sb.append("USE_INDEX_MERGE(");
				sb.append(table.getName());
				sb.append(", ");
				List<TableIndex> indexes = Randomly.nonEmptySubset(table.getIndexes());
				sb.append(indexes.stream().map(i -> i.getIndexName()).collect(Collectors.joining(", ")));
				sb.append(")");
			} else {
				throw new IgnoreMeException();
			}
			break;
		case NO_INDEX_MERGE:
			sb.append("NO_INDEX_MERGE()");
			break;
		case USE_TOJA:
			sb.append("USE_TOJA(");
			sb.append(Randomly.getBoolean());
			sb.append(")");
			break;
		}
		select.setHint(new TiDBText(sb.toString()));
	}

	private void indexesHint(String string) {
		sb.append(string);
		sb.append("(");
		List<TableIndex> indexes = tables.stream().flatMap(t -> t.getIndexes().stream()).collect(Collectors.toList());
		if (indexes.isEmpty()) {
			throw new IgnoreMeException();
		}
		List<TableIndex> indexSubset = Randomly.nonEmptySubset(indexes);
		sb.append(indexSubset.stream().map(i -> i.getIndexName()).collect(Collectors.joining(", ")));
		sb.append(")");
	}

	private void tablesHint(String string) {
		sb.append(string);
		sb.append("(");
		appendTables();
		sb.append(")");
	}

	private void appendTables() {
		List<TiDBTable> tableSubset = Randomly.nonEmptySubset(tables);
		sb.append(tableSubset.stream().map(t -> t.getName()).collect(Collectors.joining(", ")));
	}

}
