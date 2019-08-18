package com.gordan.baselibrary.dao;

import android.content.ContentValues;
import android.content.Context;


import com.gordan.baselibrary.db.SqliteHelper;
import com.gordan.baselibrary.model.TB_News;

import java.util.List;

/**
 * Created by Gordan on 2015/10/26.
 */
public class NewsDao extends SqliteHelper<TB_News>
{
    private final String TB_NAME="tb_news";

    private Context mContext=null;

    private static NewsDao instance=null;

    @Override
    protected Object convertToObj(byte[] in, Class<?> classz)
    {
        return NewsDao.class;
    }

    private NewsDao()
    {}

    public static NewsDao getInstance(Context mContext)
    {
        if(instance==null)
        {
            instance=new NewsDao();
        }
        instance.mContext=mContext;
        return instance;
    }


    public List<TB_News> getAllNews(String[] columns, String selection, String[] selectionArgs) throws Exception
    {
        return super.queryAllList(mContext, TB_News.class,TB_NAME,columns,selection,selectionArgs);
    }

    public List<TB_News> getNewsBySql(String sql) throws Exception
    {
        return super.queryBySql(mContext, TB_News.class, sql);
    }


    /***
     *分页查询
     * Author:Gordan
     *
     * @param columns
     * @param selection  查询条件 如：( id=?)
     * @param selectionArgs 条件里的参数 用来替换"?" 第1个参数，代表第1个问号；第2个参数，代表第2个问号；依此类推......
     * @param orderBy 排序 如 id desc
     * @param pageNo 页码 注意第一页是从 1 开始
     * @param pageSize 每页 数据的总数
     * @return
     */
    public List<TB_News> pageListNews(String[] columns, String selection, String[] selectionArgs,
                                      String orderBy, Integer pageNo, Integer pageSize) throws Exception
    {
       return super.queryPageList(mContext, TB_News.class, TB_NAME, columns, selection, selectionArgs, orderBy, pageNo, pageSize);

    }



    public int deleteNews(String whereClause, String[] whereArgs) throws Exception
    {
        return super.delete(mContext,TB_NAME,whereClause,whereArgs);
    }


    /***
     *
     * 根据查询的条件更新数据
     * Author:Gordan
     * @param values 表示要修改的列，如： name="steven" 即 values.put("name","Tom")
     * @param whereClause 修改条件 如：( id=?)
     * @param whereArgs 条件里的参数 用来替换"?" 第1个参数，代表第1个问号；第2个参数，代表第2个问号；依此类推......
     * @return 返回收影响的行数
     */
    public int updateNews(ContentValues values, String whereClause, String[] whereArgs) throws Exception
    {
        return super.update(mContext,TB_NAME,values,whereClause,whereArgs);
    }

    public long insertNews(TB_News bean) throws Exception
    {
        ContentValues values=new ContentValues();
        values.put("title",bean.getTitle());
        values.put("content",bean.getContent());
        values.put("createtime",bean.getCreatetime());
        values.put("category",bean.getCategory());

        return super.insert(mContext,TB_NAME,values);
    }
}
