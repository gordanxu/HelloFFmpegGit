package com.gordan.baselibrary.db;

/**
 * 所有本地数据库常量类
 * 
 *
 * 
 */
public class DbConsts
{
	public static final int DB_VERSION = 1;
	public static final String DB_NAME = "framework.db";

	public static final String TB_News="CREATE TABLE IF NOT EXISTS " +
			"tb_news(id integer primary key autoincrement,title varchar,content varchar,createtime long,category int);";
	private static final String TB_Collect = "CREATE TABLE IF NOT EXISTS "
			+"tb_collect(type int,dataType int,houseId varchar,resblockId varchar,createtime long ,data BLOB)";
	private static final String TB_Visit = "CREATE TABLE IF NOT EXISTS "
			+"tb_visit(type int,dataType int,houseId varchar,resblockId varchar,createtime long ,data BLOB)";
	private static final String TB_Share = "CREATE TABLE IF NOT EXISTS "
			+"tb_share(type int,dataType int,houseId varchar,resblockId varchar,createtime long ,data BLOB)";

	public static final String[] DDL_CRTS =	{TB_News, TB_Collect,TB_Share,TB_Visit };



}
