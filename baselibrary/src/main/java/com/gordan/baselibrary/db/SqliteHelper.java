package com.gordan.baselibrary.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gordan.baselibrary.util.LogUtils;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
/**
 * 本笃数据库操作框架
 * @author smalls
 *
 * @param <T>
 */
public abstract class SqliteHelper<T>
{
	public static final String TAG = SqliteHelper.class.getName();
	private SQLiteDatabase mSqLiteDatabase;
	private DataBaseHelper mDbHelper=null;

	/**
	 * 插入数据
	 * 
	 * @param table
	 *            表名
	 * @param values
	 *            ContentValues对象
	 * @return 返回当前行ID值，如果失败返回-1
	 */
	protected long insert(Context mContext,String table, ContentValues values) throws Exception
	{
		long num = 0l;
		open(mContext);
		LogUtils.i(TAG,"===insert===values",false);
		num = mSqLiteDatabase.insert(table, null, values);
		close();
		return num;
	}


	/***
	 * 插入一条数据
	 *
	 * @param sql
	 * @return
	 */
	protected void insert(Context mContext,String sql) throws Exception
	{
		open(mContext);
		LogUtils.i(TAG,"===insert===sql",false);
		mSqLiteDatabase.execSQL(sql);
		close();

	}

	/**
	 * 删除数据
	 * 
	 * @param sql
	 */
	protected void delete(Context mContext,String sql) throws Exception
	{
		open(mContext);
		LogUtils.i(TAG,"===delete===sql",false);
		mSqLiteDatabase.execSQL(sql);
		close();
	}

	/**
	 * 删除表中的记录
	 * 
	 * @param table
	 *            表名
	 * @param whereClause
	 *            删除条件 如：( id>? and time>?)
	 * @param whereArgs
	 *            条件里的参数 用来替换"?" 第1个参数，代表第1个问号；第2个参数，代表第2个问号；依此类推......
	 * @return 返回删除的条数
	 */
	protected int delete(Context mContext,String table, String whereClause, String[] whereArgs) throws Exception
	{
		int num = 0;
		open(mContext);
		LogUtils.i(TAG,"===delete===table",false);
		num = mSqLiteDatabase.delete(table, whereClause, whereArgs);
		close();
		return num;
	}

	/**
	 * 修改数据
	 * 
	 * @param table
	 *            表名
	 * @param values
	 *            ContentValues对象 表示要修改的列，如： name="steven" 即 values.put("name",
	 *            "steven");
	 * @param whereClause
	 *            修改条件 如：( id=?)
	 * @param whereArgs
	 *            条件里的参数 用来替换"?" 第1个参数，代表第1个问号；第2个参数，代表第2个问号；依此类推......
	 * @return 返回修改的条数
	 */
	protected int update(Context mContext,String table, ContentValues values, String whereClause, String[] whereArgs) throws Exception
	{
		int num = 0;
		open(mContext);
		LogUtils.i(TAG,"===update===table",false);
		num = mSqLiteDatabase.update(table, values, whereClause, whereArgs);
		close();
		return num;
	}

	protected void update(Context mContext,String sql) throws Exception
	{
		open(mContext);
		LogUtils.i(TAG,"===update===sql",false);
		mSqLiteDatabase.execSQL(sql);
		close();
	}

	/**
	 * 查询数据 单个对象
	 * 
	 * @param classObj
	 *            字节码 如：String.class
	 * @param table
	 *            表名
	 * @param columns
	 *            要查询的列名
	 * @param selection
	 *            查询条件 如：( id=?)
	 * @param selectionArgs
	 *            条件里的参数，用来替换"?"
	 * @return 返回Object
	 */
	@SuppressWarnings("unchecked")
	public T queryObject(Context mContext,Class<?> classObj, String table, String[] columns, String selection, String[] selectionArgs) throws Exception
	{
		Cursor c = null;
		T t = null;
		open(mContext);
		c = mSqLiteDatabase.query(table, columns, selection, selectionArgs, null, null, null, "1");
		if (c.moveToFirst())
		{
			// 生成新的实例
			t = (T) classObj.newInstance();
			columnToField(t, c);
		}
		c.close();
		close();
		return t;
	}

	/**
	 * 
	 * @param classObj
	 *            字节码 如：String.class
	 * @param sql
	 *            查询的语句
	 * @return 返回List<Object>
	 */
	@SuppressWarnings("unchecked")
	protected List<T> queryBySql(Context mContext,Class<?> classObj, String sql) throws Exception
	{
		List<T> list = new ArrayList<T>();
		Cursor c = null;
		T t = null;
		open(mContext);
		c = mSqLiteDatabase.rawQuery(sql, null);
		while (c.moveToNext())
		{
			t = (T) classObj.newInstance();
			columnToField(t, c);
			list.add(t);
		}
		c.close();
		close();
		return list;
	}

	/**
	 * 
	 * @param classObj
	 *            字节码 如：String.class
	 * @param table
	 *            表名
	 * @param columns
	 *            要查询的列名
	 * @param selection
	 *            查询条件 如：( id=?)
	 * @param selectionArgs
	 *            条件里的参数，用来替换"?"
	 * @return 返回List<Object>
	 */
	@SuppressWarnings("unchecked")
	protected List<T> queryAllList(Context mContext,Class<?> classObj, String table, String[] columns, String selection, String[] selectionArgs) throws Exception
	{
		Cursor c = null;
		T t = null;
		List<T> list = new ArrayList<T>();
		open(mContext);
		c = mSqLiteDatabase.query(table, columns, selection, selectionArgs, null, null, null);
		while (c.moveToNext())
		{
			t = (T) classObj.newInstance();
			columnToField(t, c);
			list.add(t);
		}
		c.close();
		close();
		return list;
	}

	/**
	 * 查询数据 带分页功能
	 * 
	 * @param classz
	 *            字节码 如：String.class
	 * @param table
	 *            表名
	 * @param columns
	 *            要查询的列名
	 * @param selection
	 *            查询条件 如：( id=?)
	 * @param selectionArgs
	 *            条件里的参数，用来替换"?"
	 * @param orderBy
	 *            排序 如：id desc
	 * @param pageNo
	 *            页码 不分页时，为null
	 * @param pageSize
	 *            每页的个数 不分页时，为null
	 * @return 返回List
	 */
	@SuppressWarnings("unchecked")
	protected List<T> queryPageList(Context mContext,Class<?> classz, String table, String[] columns, String selection, String[] selectionArgs,
			String orderBy, Integer pageNo, Integer pageSize) throws Exception
	{
		List<T> list =new ArrayList<T>();
		Cursor c = null;
		// 分页
		if (!(pageNo == null || pageSize == null))
		{
			// 分页的起始位置
			int begin = (pageNo - 1) * pageSize;
			orderBy = orderBy + " limit " + begin + ", " + pageSize;
		}
		// 查询数据
		open(mContext);
		c = mSqLiteDatabase.query(table, columns, selection, selectionArgs, null, null, orderBy);
		T t = null;
		while (c.moveToNext())
		{
			// 生成新的实例
			t = (T) classz.newInstance();
			// 把列的值，转换成对象里属性的值
			columnToField(t, c);
			list.add(t);
		}
		c.close();
		close();

		return list;
	}


	/**
	 * 把列的值，转换成对象里属性的值
	 * 
	 * @param t
	 * @param c
	 */
	private void columnToField(T t, Cursor c) throws Exception
	{
		// 获取T里的所有属性
		List<Field> listField = new ArrayList<Field>();
		getClassFields(t.getClass(), listField);
		for (int i = 0; i < listField.size(); i++)
		{
			int columnIndex = c.getColumnIndex(listField.get(i).getName());
			// 如果为-1，表示不存在此列
			if (columnIndex == -1)
			{
				continue;
			}
			Class<?> classz = listField.get(i).getType();
			// 设置成可访问，否则不能set值
			listField.get(i).setAccessible(true);
			if (classz == Integer.TYPE)
			{ // int
				listField.get(i).set(t, c.getInt(columnIndex));
			} else if (classz == String.class)
			{ // String
				listField.get(i).set(t, c.getString(columnIndex));
			} else if (classz == Long.TYPE)
			{ // long
				listField.get(i).set(t, c.getLong(columnIndex));
			} else if (classz == byte[].class)
			{ // byte
				listField.get(i).set(t, c.getBlob(columnIndex));
			} else if (classz == Float.TYPE)
			{ // float
				listField.get(i).set(t, c.getFloat(columnIndex));
			} else if (classz == Double.TYPE)
			{ // double
				listField.get(i).set(t, c.getDouble(columnIndex));
			} else if (classz == Short.TYPE)
			{ // short
				listField.get(i).set(t, c.getShort(columnIndex));
			} else
			{
				byte[] in = c.getBlob(columnIndex);
				Object obj = convertToObj(in, classz);
				listField.get(i).set(t, obj);
			}
		}
	}

	/**
	 * 通过遍历获得类及父类的所有属性
	 * 
	 * @param clazz
	 *            要遍历的类
	 * @param list
	 *            返回的集合
	 */
	private void getClassFields(Class<?> clazz, List<Field> list)
	{
		Field[] declaredFields = clazz.getDeclaredFields();
		for (Field field : declaredFields)
		{
			list.add(field);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
		{
			getClassFields(superclass, list);
		}
	}

	abstract protected Object convertToObj(byte[] in, Class<?> classz);

	/**
	 * 打开连接
	 */
	private void open(Context mContext) throws Exception
	{
		if(mDbHelper==null)
		{
			mDbHelper =DataBaseHelper.getInstance(mContext);
		}
		LogUtils.i(TAG,"===open===",false);
		mSqLiteDatabase = mDbHelper.getWritableDatabase();
	}

	/**
	 * 关闭连接
	 */
	private void close() throws Exception
	{
		if (mSqLiteDatabase != null && mSqLiteDatabase.isOpen())
		{
			LogUtils.i(TAG,"===close===",false);
			mSqLiteDatabase.close();
			mSqLiteDatabase=null;
		}
	}

	public byte[] getSerializable(Object obj)
	{
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		try
		{
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		} finally
		{
			try
			{
				if (bos != null)
				{
					bos.close();
				}
				if (oos != null)
				{
					oos.close();
				}
			} catch (Exception ee)
			{
				ee.printStackTrace();
			}
		}
		return bos.toByteArray();
	}
}
