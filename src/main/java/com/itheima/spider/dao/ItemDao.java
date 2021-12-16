package com.itheima.spider.dao;

import com.itheima.spider.pojo.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemDao extends JpaRepository<Item,Long> {

}
