package com.gordan.baselibrary.model;

import java.io.Serializable;

/**
 * Created by Gordan on 2015/10/26.
 *
 * 对应数据库的News表
 *
 */
public class TB_News implements Serializable
{
    public  int id;

    public String title;

    public String content;

    public long createtime;

    public int category;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }
}
