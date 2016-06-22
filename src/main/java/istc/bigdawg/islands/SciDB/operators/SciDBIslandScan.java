package istc.bigdawg.islands.SciDB.operators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import istc.bigdawg.islands.OperatorVisitor;
import istc.bigdawg.islands.PostgreSQL.utils.SQLExpressionUtils;
import istc.bigdawg.islands.SciDB.SciDBArray;
import istc.bigdawg.islands.operators.Operator;
import istc.bigdawg.islands.operators.Scan;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;

public class SciDBIslandScan extends SciDBIslandOperator implements Scan {
	
	private Expression filterExpression = null;
//	private Expression indexCond = null;
	private String srcTable = null;
	private String operatorName = null;
	
	private String tableAlias;  //may be query-specific, need to derive it here
	protected Table table;
	private boolean hasFunctionInFilterExpression = false;

//	public String getJoinPredicate(){
//		return getIndexCond() != null ? getIndexCond().toString(): null;
//	}

	
//	public SciDBIslandScan(Map<String, String> parameters, List<String> output, SciDBIslandOperator child, SQLTableExpression supplement) throws Exception {
//		super(parameters, output, child, supplement);
//
//		isBlocking = false;
//
//		setSrcTable(parameters.get("Relation-Name"));
//		
//		if(getSrcTable() == null) { // it's a cte scan
//			setSrcTable(parameters.get("CTE-Name"));
//		}
//		setTableAlias(parameters.get("Alias"));
//		
//		if (parameters.get("Filter") != null) {
//			
//			String s = SQLExpressionUtils.removeExpressionDataTypeArtifactAndConvertLike(parameters.get("Filter"));
//			
//			setFilterExpression(CCJSqlParserUtil.parseCondExpression(s));
//			SQLExpressionUtils.removeExcessiveParentheses(filterExpression);
//			
//			setHasFunctionInFilterExpression(SQLExpressionUtils.isFunctionPresentInCondExpression(filterExpression));
////			System.out.println("---> filterExpression: "+filterExpression);
////			
////			filterSet = new HashSet<Expression>();
////			filterSet.add(filterExpression);
//			
//		}
//		
//		if (parameters.get("Index-Cond") != null) {
//			String s = SQLExpressionUtils.removeExpressionDataTypeArtifactAndConvertLike(parameters.get("Index-Cond"));
//			
//			setIndexCond(CCJSqlParserUtil.parseCondExpression(s));
//			SQLExpressionUtils.removeExcessiveParentheses(indexCond);
//			
//			List<Expression> exprs = SQLExpressionUtils.getFlatExpressions(getIndexCond());
//			Expression result = null; 
//			for (Expression e : exprs) {
//				if (SQLExpressionUtils.getAttributes(e).size() == 1) {
//					if (filterExpression == null)
//						filterExpression = e;
//					else 
//						filterExpression = new AndExpression(filterExpression, e);
//					continue;
//				}
//				if (result == null)
//					result = e; 
//				else 
//					result = new AndExpression(result, e);
//			}
//			indexCond = result;
//			
////			System.out.println("---> indexCond: "+indexCond);
//			
////			if (filterSet == null) filterSet = new HashSet<Expression>();
////			filterSet.add(indexCond);
//		}
//		
//		table = new Table(getSrcTable()); // new one to accommodate aliasing
//		if (parameters.get("Schema") != null && (!parameters.get("Schema").equals("public"))) 
//			table.setSchemaName(parameters.get("Schema"));
//
//		if(getTableAlias() != null && !getTableAlias().equalsIgnoreCase(getSrcTable())) {
//			table.setAlias(new Alias(getTableAlias()));
//		}
//	}
	
	// for AFL
	public SciDBIslandScan(Map<String, String> parameters, SciDBArray output, Operator child) throws Exception {
		super(parameters, output, child);

		isBlocking = false;

		setSourceTableName(parameters.get("Relation-Name"));
		
		if(getSourceTableName() == null) { // it's a cte scan
			setSourceTableName(parameters.get("CTE-Name"));
		}
		setTableAlias(parameters.get("Alias"));
		
		if(parameters.get("Filter") != null) {
			
			setFilterExpression(CCJSqlParserUtil.parseCondExpression(parameters.get("Filter")));
			setHasFunctionInFilterExpression(SQLExpressionUtils.isFunctionPresentInCondExpression(filterExpression));
		}
		
		table = new Table(getSourceTableName()); // new one to accommodate aliasing
		if (parameters.get("Schema") != null && (!parameters.get("Schema").equals("public"))) 
			table.setSchemaName(parameters.get("Schema"));

		if(getTableAlias() != null && !getTableAlias().equalsIgnoreCase(getSourceTableName())) {
			table.setAlias(new Alias(getTableAlias()));
		}

		
	}
	
	public SciDBIslandScan(SciDBIslandOperator o, boolean addChild) throws Exception {
		super(o, addChild);
		SciDBIslandScan sc = (SciDBIslandScan) o;
		
		if (sc.getFilterExpression() != null) 
			this.setFilterExpression(CCJSqlParserUtil.parseCondExpression(sc.getFilterExpression().toString()));
		this.setSourceTableName(new String(sc.getSourceTableName()));
		this.setTableAlias(new String(sc.getTableAlias()));
		this.setHasFunctionInFilterExpression(sc.isHasFunctionInFilterExpression());

		this.table = new Table();
		try {
			this.table.setName(new String(sc.table.getName()));
			if (sc.table.getSchemaName() != null) this.table.setSchemaName(new String(sc.table.getSchemaName()));
			if (sc.table.getAlias() != null) this.table.setAlias(sc.table.getAlias());
			if (sc.table.getASTNode() != null) this.table.setASTNode(sc.table.getASTNode());
			if (sc.table.getDatabase() != null)this.table.setDatabase(sc.table.getDatabase());
			if (sc.table.getPivot() != null)this.table.setPivot(sc.table.getPivot());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Table getTable() {
		return table;
		
	}
	
	
//	@Override
//	public String generateAFLString(int recursionLevel) throws Exception {
//		StringBuilder sb = new StringBuilder();
//		if (!(getOperatorName().equals("scan") && recursionLevel > 0))
//			sb.append(getOperatorName()).append('(');
//		
//		boolean ped = (!this.getChildren().isEmpty()) && this.getChildren().get(0).isPruned();
//		
//		if (ped)
//			sb.append(this.getChildren().get(0).getPruneToken());
//		else if (children.size() > 0)
//			sb.append(children.get(0).generateAFLString(recursionLevel + 1));
//		else {
//			sb.append(getSrcTable());
//		} 
//		
//		switch (getOperatorName()) {
//		case "apply":
//			for (String s : outSchema.keySet()){
//				if (outSchema.get(s).isHidden()) continue;
//				if (outSchema.get(s).getName().equals(outSchema.get(s).getExpressionString())) continue;
//				sb.append(", ").append(s).append(", ");
////					if (ped) {
////						// the apply
////						Expression expression = CCJSqlParserUtil.parseExpression(outSchema.get(s).getExpressionString());
////						Set<String> nameSet = new HashSet<>( SQLExpressionUtils.getColumnTableNamesInAllForms(expression));
////						SQLExpressionUtils.renameAttributes(expression, nameSet, nameSet, getChildren().get(0).getPruneToken());
////						sb.append(expression.toString());
////					} else {
//					sb.append(outSchema.get(s).getExpressionString());
////					}
//			}
//			
//			break;
//		case "project":
//			for (String s : outSchema.keySet()){
//				if (outSchema.get(s).isHidden()) continue;
//				
//				sb.append(", ");
//				if (ped) {
//					String[] o = outSchema.get(s).getName().split("\\.");
//					sb.append(getChildren().get(0).getPruneToken()).append('.').append(o[o.length-1]);
//				} else 
//					sb.append(outSchema.get(s).getName());
//			}
//			break;
//		case "redimension":
//			sb.append(", <");
//			
//			for (String s : outSchema.keySet()) {
//				if (outSchema.get(s).isHidden()) continue;
//				if (sb.charAt(sb.length()-1) != '<') sb.append(',');
//				sb.append(outSchema.get(s).generateAFLTypeString());
//			}
//			sb.append(">[");
//			for (String s : outSchema.keySet()) {
//				if (!outSchema.get(s).isHidden()) continue;
//				if (sb.charAt(sb.length()-1) != '[') sb.append(',');
//				sb.append(outSchema.get(s).generateAFLTypeString());
//			}
//			sb.append(']');
//			break;
//		case "scan":
//			break;
//		case "filter":
//			sb.append(", ").append(getFilterExpression());
//			break;
//		default:
//			break;
//		}
//			
//		if (!(getOperatorName().equals("scan") && recursionLevel > 0))
//			sb.append(')');
//		return sb.toString();
//	}
	
	

	@Override
	public Map<String, List<String>> getTableLocations(Map<String, List<String>> locations) {
		Map<String, List<String>> result = new HashMap<>();
		if (children != null) {
			for (Operator o : children)
				result.putAll(((SciDBIslandOperator)o).getTableLocations(locations));
		}
		String schemaAndName = table.getName();
		if (table.getSchemaName() != null) schemaAndName = table.getSchemaName() + "." +schemaAndName;
		result.put(schemaAndName, locations.get(schemaAndName));
		return result;
	}
	
//	@Override
//	public Map<String, Expression> getChildrenPredicates() throws Exception {
//		Map<String, Expression> ret = new HashMap<>();
//		ret.put((this.getTableAlias() != null ? this.getTableAlias() : this.getSrcTable()), getIndexCond());
//		return ret;
//	}
//	
//	
	@Override
	public Map<String, Set<String>> getObjectToExpressionMappingForSignature() throws Exception{
		
		if (! children.isEmpty()) return super.getObjectToExpressionMappingForSignature();
		
		Operator parent = this;
		while (!parent.isBlocking() && parent.getParent() != null ) parent = parent.getParent();
		Map<String, String> aliasMapping = parent.getDataObjectAliasesOrNames();
		
		Map<String, Set<String>> out = new HashMap<>();
		
		// filter
		if (getFilterExpression() != null && !SQLExpressionUtils.containsArtificiallyConstructedTables(getFilterExpression())) {
			addToOut(CCJSqlParserUtil.parseCondExpression(getFilterExpression().toString()), out, aliasMapping);
		}
		
		return out;
	}
	
	
	
//	@Override
//	public void seekScanAndProcessAggregateInFilter() throws Exception {
//		
//		if (getFilterExpression() == null) return;
//		
//		if (!SQLExpressionUtils.isFunctionPresentInCondExpression(getFilterExpression())) return;
//		
//		List<Expression> exp = SQLExpressionUtils.locateFunctionInCondExpression(getFilterExpression());
//		while (!exp.isEmpty()) {
//			StringBuilder sb = new StringBuilder();
//			Set<String> names = new HashSet<>();
//			Expression result = resolveAggregatesInFilter(exp.get(0).toString(), true, this, names, sb);
//			if (result != null) {
//				SQLExpressionUtils.updateFunctionInCondExpression(result, exp.get(1));
//				exp = SQLExpressionUtils.locateFunctionInCondExpression(getFilterExpression());
//				SQLExpressionUtils.renameAttributes(getIndexCond(), names, null, sb.toString());
//				
//			} else {
//				break;
//			}
//		}
//	}

	@Override
	public void accept(OperatorVisitor operatorVisitor) throws Exception {
		operatorVisitor.visit(this);
	}

	public Expression getFilterExpression() {
		return filterExpression;
	}


	public void setFilterExpression(Expression filterExpression) {
		this.filterExpression = filterExpression;
	}


	public boolean isHasFunctionInFilterExpression() {
		return hasFunctionInFilterExpression;
	}


	public void setHasFunctionInFilterExpression(boolean hasFunctionInFilterExpression) {
		this.hasFunctionInFilterExpression = hasFunctionInFilterExpression;
	}


//	public String getSrcTable() {
//		return srcTable;
//	}
//
//
//	public void setSrcTable(String srcTable) {
//		this.srcTable = srcTable;
//	}


	public String getTableAlias() {
		return tableAlias;
	}


	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}


	public String getOperatorName() {
		return operatorName;
	}


	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}


//	public Expression getIndexCond() {
//		return indexCond;
//	}
//
//
//	public void setIndexCond(Expression indexCond) {
//		this.indexCond = indexCond;
//	}


	@Override
	public String getSourceTableName() {
		return srcTable;
	}


	@Override
	public void setSourceTableName(String srcTableName) {
		this.srcTable = srcTableName;
	}


	@Override
	public String generateRelevantJoinPredicate() throws Exception {
		// because there will not be a joinPredicated embedded
		return null;
	}

	
	
}
