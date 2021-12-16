package com.itheima.spider.service;

import com.itheima.spider.pojo.Item;

import java.util.List;

public interface ItemService {

    public void save(Item item);

    public List<Item> findAll(Item item);
}
