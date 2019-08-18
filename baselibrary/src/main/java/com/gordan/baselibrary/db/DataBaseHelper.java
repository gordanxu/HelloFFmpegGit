package com.gordan.baselibrary.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.gordan.baselibrary.util.LogUtils;

/**
 * 本地数据库父类
 * 用于更新和升级本地数据库表结构
 * @author smalls
 *
 */
public class DataBaseHelper extends SQLiteOpenHelper
{
	final static String TAG=DataBaseHelper.class.getSimpleName();

	private static DataBaseHelper instance=null;

	public static DataBaseHelper getInstance(Context context)
	{
		if(instance == null)
		{
			synchronized(DataBaseHelper.class)
			{
				instance=new DataBaseHelper(context);
			}
		}
		return instance;
	}

	private DataBaseHelper(Context context)
	{
		this(context, DbConsts.DB_NAME, null, DbConsts.DB_VERSION);
	}

	private DataBaseHelper(Context context, String name, CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		LogUtils.i(TAG,"===onCreate()===",false);
		for (int i = 0; i < DbConsts.DDL_CRTS.length; i++)
		{
			db.execSQL(DbConsts.DDL_CRTS[i]);
			LogUtils.i(TAG,"=====create "+DbConsts.DDL_CRTS[i]+"===success",false);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//当数据库升级 需要增加表 或者 增加某张表的字段时 可以根据版本号 执行相应的SQL语句
		LogUtils.i(TAG,"===onUpgrade()===",false);
	}
}
