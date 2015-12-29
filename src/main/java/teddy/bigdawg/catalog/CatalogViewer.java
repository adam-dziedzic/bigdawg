package teddy.bigdawg.catalog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import istc.bigdawg.postgresql.PostgreSQLConnectionInfo;

public class CatalogViewer {

	private static Logger logger = Logger.getLogger(CatalogViewer.class.getName());

	/**
	 * With a CSV String of terms, fetch those that stands for an object. Used
	 * in within-island parser.
	 * 
	 * @param cc
	 * @param csvstr
	 * @return String of TSV String of object names (obj)
	 * @throws Exception
	 */
	public static String getObjectsFromList(Catalog cc, String csvstr) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (csvstr.length() == 0)
			return "";

		String[] strs = csvstr.split(",");
		int len = strs.length;
		String extraction = new String("");

		String wherePred = new String(" lower(o.name) = lower(\'" + strs[0] + "\') ");
		for (int i = 1; i < len; i++) {
			wherePred = wherePred + "or lower(o.name) = lower(\'" + strs[i] + "\') ";
		}

		ResultSet rs = cc.execRet(
				"select distinct o.name obj " + "from catalog.objects o " + "where " + wherePred + "order by o.name;");
		if (rs.next())
			extraction = extraction + rs.getString("obj");
		while (rs.next()) {
			extraction = extraction + "\t" + rs.getString("obj");
		}
		rs.close();

		return extraction;
	}

	/**
	 * View all shims stored in catalog.
	 * 
	 * @param cc
	 * @return ArrayList of TSV String of shim_id, island name, engine name and
	 *         shim access_method
	 * @throws Exception
	 */
	public static ArrayList<String> getAllShims(Catalog cc) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select shim_id, i.scope_name island, e.name engine, sh.access_method "
				+ "from catalog.shims sh " + "join catalog.islands i on sh.island_id = i.iid "
				+ "join catalog.engines e on e.eid = sh.engine_id;");
		while (rs.next()) {
			extraction.add(rs.getString("shim_id") + "\t" + rs.getString("island") + "\t" + rs.getString("engine")
					+ "\t" + rs.getString("access_method"));
		}

		return extraction;
	}

	/**
	 * View all casts stored in catalog.
	 * 
	 * @param cc
	 * @return ArrayList of TSV String of src engine name (src), dst engine name
	 *         (dst), and cast access_method
	 * @throws Exception
	 */
	public static ArrayList<String> getAllCasts(Catalog cc) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select e1.name src, e2.name dst, c.access_method " + "from catalog.casts c "
				+ "join catalog.engines e1 on c.src_eid = e1.eid " + "join catalog.engines e2 on c.dst_eid = e2.eid;");
		while (rs.next()) {
			extraction.add(rs.getString("src") + "\t" + rs.getString("dst") + "\t" + rs.getString("access_method"));
		}

		return extraction;
	}

	/**
	 * View all objects stored in catalog.
	 * 
	 * @param cc
	 * @return ArrayList of TSV String of object name (obj), fields, dbid of
	 *         physical_db, and name of engine (engine)
	 * @throws Exception
	 */
	public static ArrayList<String> getAllObjects(Catalog cc) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select o.name obj, o.fields, d.name physical_db, e.name engine "
				+ "from catalog.objects o " + "left join catalog.databases d 	on o.physical_db = d.dbid "
				+ "join catalog.engines e 			on d.engine_id = e.eid;");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("fields") + "\t" + rs.getString("physical_db")
					+ "\t" + rs.getString("engine"));
		}

		return extraction;
	}

	/**
	 * With an object's name, fetch all objects that share this name.
	 * 
	 * @param cc
	 * @param objName
	 * @return ArrayList of TSV String of object name (obj), fields,
	 *         physical_db, physical_db
	 * @throws Exception
	 */
	public static ArrayList<String> getObjectsByName(Catalog cc, String objName) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		CatalogUtilities.checkLength(objName, 15);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select o.name obj, o.fields, d1.name physical_db, d2.name physical_db "
				+ "from catalog.objects o " + "join catalog.databases d1 on o.physical_db = d1.dbid "
				+ "join catalog.databases d2 on o.physical_db = d2.dbid " + "where o.name ilike  \'%" + objName
				+ "%\';");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("fields") + "\t" + rs.getString("physical_db")
					+ "\t" + rs.getString("physical_db"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With name of the db, fetch its access information
	 * 
	 * @param cc
	 * @param db
	 * @return ArrayList of TSV String of db name (name), userid and password.
	 * @throws Exception
	 */
	public static ArrayList<String> getDbAccessInfo(Catalog cc, String dbName) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		CatalogUtilities.checkLength(dbName, 15);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet(
				"select name, userid, password " + "from catalog.databases where name ilike \'%" + dbName + "%\';");

		while (rs.next()) {
			extraction.add(rs.getString("name") + "\t" + rs.getString("userid") + "\t" + rs.getString("password"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With name of engine, fetch all db associated with it
	 * 
	 * @param cc
	 * @param engineName
	 * @return ArrayList of TSV String of db name (name) and engine name
	 *         (engine).
	 * @throws Exception
	 */
	public static ArrayList<String> getDbsOfEngine(Catalog cc, String engineName) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		CatalogUtilities.checkLength(engineName, 15);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select d.name db, e.name engine " + "from catalog.databases d "
				+ "join catalog.engines e 	on d.engine_id = e.eid " + "where e.name ilike \'%" + engineName + "%\' "
				+ "order by d.name, e.name;");

		while (rs.next()) {
			extraction.add(rs.getString("db") + "\t" + rs.getString("engine"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With island name, fetch all DBs connected to it through a shim.
	 * 
	 * @param cc
	 * @param islandName
	 * @return ArrayList of TSV String of db name (db), island name (island),
	 *         and shim access method (access_method)
	 * @throws Exception
	 */
	public static ArrayList<String> getDbsOfIsland(Catalog cc, String islandName) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		CatalogUtilities.checkLength(islandName, 15);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select d.name db, e.name engine, i.scope_name island, sh.access_method "
				+ "from catalog.databases d " + "join catalog.shims sh 	on d.engine_id = sh.engine_id "
				+ "join catalog.engines e	on d.engine_id = e.eid "
				+ "join catalog.islands i 	on sh.island_id = i.iid " + "where i.scope_name ilike \'%" + islandName
				+ "%\' " + "order by d.name, i.scope_name;");

		while (rs.next()) {
			extraction.add(rs.getString("db") + "\t" + rs.getString("engine") + "\t" + rs.getString("island") + "\t"
					+ rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With object name, fetch database name and engine name.
	 * 
	 * @param cc
	 * @param objName
	 * @return ArrayList of TSV String of object name (obj), database name (db),
	 *         and engine name (engine)
	 * @throws Exception
	 */
	public static ArrayList<String> getDbsOfObject(Catalog cc, String objName) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		CatalogUtilities.checkLength(objName, 15);

		ArrayList<String> extraction = new ArrayList<String>();

		ResultSet rs = cc.execRet("select o.name obj, d.name db, e.name engine " + "from catalog.objects o "
				+ "join catalog.databases d 	on o.physical_db = d.dbid "
				+ "join catalog.engines e 		on d.engine_id = e.eid " + "where o.name ilike \'%" + objName + "%\' "
				+ "order by o.name, d.name, e.name;");

		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("db") + "\t" + rs.getString("engine"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of objects and the corresponding islands, fetch all relevant
	 * shim informations. NOTE: if a shim is not available it will not show
	 * 
	 * @param cc
	 * @param objs
	 * @return ArrayList of TSV String of object name (obj), fields, dbid, iid,
	 *         shim_id
	 * @throws Exception
	 */
	public static ArrayList<String> getShimsUseObjectsIslands(Catalog cc, ArrayList<String> objs,
			ArrayList<String> islands) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (objs.size() == 0)
			return new ArrayList<String>();
		if (islands.size() != objs.size())
			throw new Exception("getShimsUseObjectsIslands - Lengths of object list and islands list do not match");
		for (String objName : objs)
			CatalogUtilities.checkLength(objName, 15);
		for (String islName : islands)
			CatalogUtilities.checkLength(islName, 15);

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> objsdup = new ArrayList<String>();
		objsdup.addAll(objs.subList(1, objs.size()));
		ArrayList<String> isldup = new ArrayList<String>();
		isldup.addAll(islands.subList(1, islands.size()));

		String wherePred;
		if (islands.size() == 0)
			wherePred = new String(" o.name ilike \'%" + objs.get(0) + "%\' ");
		else {
			wherePred = new String(" (o.name ilike \'%" + objs.get(0) + "%\' and isl.scope_name ilike \'%"
					+ String.join("%\' and isl.scope_name ilike \'%", islands.get(0).split(",")) + "%\') ");
		}

		for (String objName : objsdup) {
			if (isldup.get(objsdup.indexOf(objName)) == "") {
				wherePred = wherePred + "or o.name ilike \'%" + objName + "%\' ";
			} else {
				wherePred = wherePred + "or (o.name ilike \'%" + objName + "%\' and isl.scope_name ilike \'%" + String
						.join("%\' and isl.scope_name ilike \'%", isldup.get(objsdup.indexOf(objName)).split(","))
						+ "%\') ";
			}
		}

		ResultSet rs = cc.execRet("select o.name obj, o.fields, d.name db, isl.scope_name island, sh.access_method "
				+ "from catalog.objects o " + "join catalog.databases d  		on o.physical_db = d.dbid "
				+ "join catalog.engines e   		on d.engine_id = e.eid "
				+ "right join catalog.shims sh   	on e.eid = sh.engine_id "
				+ "join catalog.islands isl  		on sh.island_id = isl.iid " + "where " + wherePred
				+ "order by o.name, d.name, iid, shim_id;");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("fields") + "\t" + rs.getString("db") + "\t"
					+ rs.getString("island") + "\t" + rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of object, a list of some of their fields and the list of
	 * corresponding islands, fetch all relevant shim information. Each string
	 * in the fs is a CSV string of some or all fields included in the object.
	 * If a string in fs is blank string "", all fields will be included. NOTE:
	 * if a shim does not exist, it will not show
	 * 
	 * @param cc
	 * @param objs
	 * @param fs
	 * @return ArrayList of TSV String of object name (obj), fields, db name
	 *         (db), island, and shim access method.
	 * @throws Exception
	 */
	public static ArrayList<String> getShimsUseObjectsFieldsIslands(Catalog cc, ArrayList<String> objs,
			ArrayList<String> fs, ArrayList<String> islands) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (objs.size() == 0)
			return new ArrayList<String>();
		if (fs.size() != objs.size() || islands.size() != objs.size())
			throw new Exception(
					"getShimsUseObjectsFieldsIslands - Lengths of object, fields and islands lists do not match");
		for (String objName : objs)
			CatalogUtilities.checkLength(objName, 15);
		for (String f : fs)
			CatalogUtilities.checkLength(f, 300);
		for (String islName : islands) {
			CatalogUtilities.checkLength(islName, 15);
			if (islName == "" || islName == null)
				throw new Exception("getShimsUseObjectsFieldsIslands - islands ArrayList is not well constructed.");
		}

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> objsdup = new ArrayList<String>();
		objsdup.addAll(objs.subList(1, objs.size()));
		ArrayList<String> fsdup = new ArrayList<String>();
		fsdup.addAll(fs.subList(1, fs.size()));
		ArrayList<String> isldup = new ArrayList<String>();
		isldup.addAll(islands.subList(1, islands.size()));
		String wherePred;
		if (fs.size() == 0)
			wherePred = new String(
					" o.name ilike \'%" + objs.get(0) + "%\' and i.scope_name ilike \'%" + islands.get(0) + "%\' ");
		else {
			wherePred = new String(" (o.name ilike \'%" + objs.get(0) + "%\' and o.fields ilike \'%"
					+ String.join("%\' and o.fields ilike \'%", fs.get(0).split(",")) + "%\' and i.scope_name ilike \'%"
					+ islands.get(0) + "%\') ");
		}

		for (String objName : objsdup) {
			if (fsdup.get(objsdup.indexOf(objName)) == "") {
				wherePred = wherePred + "or (o.name ilike \'%" + objName + "%\' and i.scope_name ilike \'%"
						+ isldup.get(objsdup.indexOf(objName)) + "%\') ";
			} else {
				wherePred = wherePred + "or (o.name ilike \'%" + objName + "%\' and o.fields ilike \'%"
						+ String.join("%\' and o.fields ilike \'%", fsdup.get(objsdup.indexOf(objName)).split(","))
						+ "%\' " + "and i.scope_name ilike \'%" + isldup.get(objsdup.indexOf(objName)) + "%\') ";
			}
		}

		ResultSet rs = cc.execRet("select o.name obj, o.fields, d.name db, i.scope_name island, sh.access_method "
				+ "from catalog.objects o " + "join catalog.databases d  		on o.physical_db = d.dbid "
				+ "join catalog.engines e   		on d.engine_id = e.eid "
				+ "right join catalog.shims sh   	on e.eid = sh.engine_id "
				+ "join catalog.islands i	  		on sh.island_id = i.iid " + "where " + wherePred
				+ "order by o.name, fields, d.name, i.scope_name;");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("fields") + "\t" + rs.getString("db") + "\t"
					+ rs.getString("island") + "\t" + rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of objects, fetch all relevant one-step casts. Useful for
	 * migrating intermediate results; input one local table NOTE: if a cast is
	 * unavailable it will not show.
	 * 
	 * @param cc
	 * @param objs
	 * @return ArrayList of TSV String of dbName (db), source engine id
	 *         (src_id), destination engine id (src_id)
	 * @throws Exception
	 */
	public static ArrayList<String> getOneStepCastsUseObjects(Catalog cc, ArrayList<String> objs) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (objs.size() == 0)
			return new ArrayList<String>();
		for (String objName : objs)
			CatalogUtilities.checkLength(objName, 15);

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> objsdup = new ArrayList<String>();
		objsdup.addAll(objs.subList(1, objs.size()));
		String wherePred = new String(" o.name ilike \'%" + objs.get(0) + "%\' ");
		for (String objName : objsdup) {
			wherePred = wherePred + "or o.name ilike \'%" + objName + "%\' ";
		}

		ResultSet rs = cc.execRet("select distinct o.name obj, e1.name src, e2.name dst, c.access_method "
				+ "from catalog.objects o " + "join catalog.databases d 	on o.physical_db = d.dbid "
				+ "join catalog.casts c 		on c.src_eid = d.engine_id "
				+ "join catalog.engines e1		on c.src_eid = e1.eid "
				+ "join catalog.engines e2		on c.dst_eid = e2.eid " + "where " + wherePred
				+ " and c.src_eid != c.dst_eid " + "order by o.name, e1.name, e2.name;");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("src") + "\t" + rs.getString("dst") + "\t"
					+ rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of engines, fetch all relevant one-step casts.
	 * 
	 * @param cc
	 * @param src
	 * @param dst
	 * @return ArrayList of TSV String of source engine name (src), destination
	 *         engine name (dst) and cast access method (access_method)
	 * @throws Exception
	 */
	public static ArrayList<String> getOneStepCastsUseEngineNames(Catalog cc, ArrayList<String> src_e,
			ArrayList<String> dst_e) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (src_e.size() == 0)
			return new ArrayList<String>();
		if (src_e.size() != dst_e.size())
			throw new Exception(
					"getOneStepCastsUseEngineNames - Lengths of source list and destination list do not match");
		for (String eName : src_e)
			CatalogUtilities.checkLength(eName, 15);
		for (String eName : dst_e)
			CatalogUtilities.checkLength(eName, 15);

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> srcdup = new ArrayList<String>();
		srcdup.addAll(src_e.subList(1, src_e.size()));
		ArrayList<String> dstdup = new ArrayList<String>();
		dstdup.addAll(dst_e.subList(1, dst_e.size()));
		String wherePred = new String(
				"(e1.name ilike \'%" + src_e.get(0) + "%\' and e2.name ilike \'%" + dst_e.get(0) + "%\') ");
		for (String eName : srcdup) {
			wherePred = wherePred + "or (e1.name ilike \'%" + eName + "%\' and e2.name ilike \'%"
					+ dstdup.get(srcdup.indexOf(eName)) + "%\') ";
		}

		ResultSet rs = cc.execRet("select distinct e1.name src, e2.name dst, c.access_method "
				+ "from catalog.engines e1 " + "join catalog.casts c 	on e1.eid = c.src_eid "
				+ "join catalog.engines e2 	on e2.eid = c.dst_eid " + "where " + wherePred
				+ "order by e1.name, e2.name;");
		while (rs.next()) {
			extraction.add(rs.getString("src") + "\t" + rs.getString("dst") + "\t" + rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of source and destination db ids, fetch shim access method
	 * 
	 * @param cc
	 * @param src_db
	 * @param dst_db
	 * @return ArrayList of TSV String of source database id (src_db),
	 *         destination database id (dst_db), source engine (src_ename),
	 *         destination engine (dst_ename) and cast access method
	 *         (access_method)
	 * @throws Exception
	 */
	public static ArrayList<String> getOneStepCastsUseDbToDb(Catalog cc, ArrayList<String> src_db,
			ArrayList<String> dst_db) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (src_db.size() == 0)
			return new ArrayList<String>();
		if (src_db.size() != dst_db.size())
			throw new Exception("getOneStepCastsUseDbToDb - Lengths of source list and destination list do not match");
		for (String dbName : src_db)
			CatalogUtilities.checkLength(dbName, 15);
		for (String dbName : dst_db)
			CatalogUtilities.checkLength(dbName, 15);

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> srcdup = new ArrayList<String>();
		srcdup.addAll(src_db.subList(1, src_db.size()));
		ArrayList<String> dstdup = new ArrayList<String>();
		dstdup.addAll(dst_db.subList(1, dst_db.size()));
		String wherePred = new String(
				"(d1.name ilike \'%" + src_db.get(0) + "%\' and d2.name ilike \'%" + dst_db.get(0) + "%\') ");
		for (String dbName : srcdup) {
			wherePred = wherePred + "or (d1.name ilike \'%" + dbName + "%\' and d2.name ilike \'%"
					+ dstdup.get(srcdup.indexOf(dbName)) + "%\') ";
		}

		ResultSet rs = cc.execRet(
				"select distinct d1.name src_db, d2.name dst_db, e1.name src_engine, e2.name dst_engine, c.access_method "
						+ "from catalog.databases d1 " + "join catalog.engines e1 		on d1.engine_id = e1.eid "
						+ "join catalog.casts c 		on e1.eid = c.src_eid "
						+ "join catalog.engines e2 		on e2.eid = c.dst_eid "
						+ "join catalog.databases d2 	on e2.eid = d2.engine_id " + "where " + wherePred
						+ "order by d1.name, d2.name, e1.name, e2.name;");
		while (rs.next()) {
			extraction.add(rs.getString("src_db") + "\t" + rs.getString("dst_db") + "\t" + rs.getString("src_engine")
					+ "\t" + rs.getString("dst_engine") + "\t" + rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * With a list of objects and corresponding list of destination islands,
	 * find out where could we route data in one step
	 * 
	 * @param cc
	 * @param objs
	 * @param islands
	 * @return ArrayList of TSV String of object name (obj), source and
	 *         destination dbid (src_db, dst_db), island name (island) and cast
	 *         access_method
	 * @throws Exception
	 */
	public static ArrayList<String> getOneStepCastDbsUseObjectsIslands(Catalog cc, ArrayList<String> objs,
			ArrayList<String> islands) throws Exception {
		// input check
		CatalogUtilities.checkConnection(cc);
		if (objs.size() == 0)
			return new ArrayList<String>();
		if (objs.size() != islands.size())
			throw new Exception(
					"getObjectsOneStepCastDbsUseIsland - Lengths of objs list and islands list do not match");
		for (String objName : objs)
			CatalogUtilities.checkLength(objName, 15);
		for (String iName : islands)
			CatalogUtilities.checkLength(iName, 15);

		ArrayList<String> extraction = new ArrayList<String>();
		ArrayList<String> objdup = new ArrayList<String>();
		objdup.addAll(objs.subList(1, objs.size()));
		ArrayList<String> isldup = new ArrayList<String>();
		isldup.addAll(islands.subList(1, islands.size()));
		String wherePred = new String(
				"(o.name ilike \'%" + objs.get(0) + "%\' and i.scope_name ilike \'%" + islands.get(0) + "%\') ");
		for (String objName : objdup) {
			wherePred = wherePred + "or (o.name ilike \'%" + objName + "%\' and i.scope_name ilike \'%"
					+ isldup.get(objdup.indexOf(objName)) + "%\') ";
		}

		ResultSet rs = cc
				.execRet("select o.name obj, d1.name src_db, d2.name dst_db, i.scope_name island, c.access_method "
						+ "from catalog.objects o " + "join catalog.databases d1 	on o.physical_db = d1.dbid "
						+ "join catalog.engines e1 		on d1.engine_id = e1.eid "
						+ "join catalog.casts c 		on e1.eid = c.src_eid "
						+ "join catalog.engines e2 		on c.dst_eid = e2.eid "
						+ "join catalog.databases d2 	on d2.engine_id = e2.eid "
						+ "join catalog.shims sh 		on e2.eid = sh.engine_id "
						+ "join catalog.islands i 		on sh.island_id = i.iid " + "where " + wherePred
						+ "order by o.name, d1.name, d2.name, i.scope_name;");
		while (rs.next()) {
			extraction.add(rs.getString("obj") + "\t" + rs.getString("src_db") + "\t" + rs.getString("dst_db") + "\t"
					+ rs.getString("island") + "\t" + rs.getString("access_method"));
		}
		rs.close();

		return extraction;
	}

	/**
	 * author: Adam Dziedzic
	 * 
	 * Get connection to a database based on engineId and dbId.
	 * 
	 * 
	 * @param cc
	 *            catalog with the connection to catalog to PostgreSQL
	 * @param engineId
	 *            - id of the database engine
	 * @param dbId
	 *            - id of a database in the engine
	 * @return connection information
	 * @throws Exception
	 *             problem with the connection to the catalog or no data for the
	 *             arguments in the catalog
	 */
	public static PostgreSQLConnectionInfo getConnection(Catalog cc, int engineId, int dbId) throws Exception {
		// TODO add cache for the connections (done by Adam)
		CatalogUtilities.checkConnection(cc);
		PreparedStatement stmt = cc.connection
				.prepareStatement("SELECT host,port,databases.name as name,userid,password "
						+ "FROM catalog.engines engines, catalog.databases databases "
						+ "WHERE engines.eid=databases.engine_id and engine_id=? and databases.dbid=?");
		stmt.setInt(1, engineId);
		stmt.setInt(2, dbId);
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery();
		} catch (SQLException e) {
			String msg = "database access error; this method is called on a closed PreparedStatement or"
					+ " the SQL statement does not return a ResultSet object";
			logger.error(msg);
			e.printStackTrace();
			throw e;
		}
		if (rs.next() == false) {
			String msg = "No results for the given engineId: " + engineId + " and dbId: " + dbId;
			System.err.println(msg);
			logger.error(msg);
			throw new Exception(msg);
		}
		PostgreSQLConnectionInfo conInfo = new PostgreSQLConnectionInfo(rs.getString("host"), rs.getString("port"), rs.getString("name"),
				rs.getString("userid"), rs.getString("password"));
		return conInfo;
	}

}