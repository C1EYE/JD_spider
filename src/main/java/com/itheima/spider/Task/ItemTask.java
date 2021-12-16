package com.itheima.spider.Task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.spider.pojo.Item;
import com.itheima.spider.service.ItemService;
import com.itheima.spider.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ItemTask {
    private static String keyword = "手机";
    private static int Page = 20;
    @Autowired
    private HttpUtils httpUtils;

    @Autowired
    private ItemService itemService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Scheduled(fixedDelay = 100 * 1000)
    public void itemTask() throws Exception {

        String url = "https://search.jd.com/Search?keyword="+keyword+"&page=";
        for (int i = 1; i < Page; i += 2) {
            String html = httpUtils.doGetHtml(url + i);
            parse(html);
        }

        System.out.println("完成一次爬取");
    }

    private void parse(String html) throws JsonProcessingException {
        Document document = Jsoup.parse(html);

        Elements elements = document.select("div#J_goodsList > ul > li");
        for (Element spuEle : elements) {
            String spuStr = spuEle.attr("data-spu");
            if (spuStr.length()==0) spuStr = "-1";
            long spu = Long.parseLong(spuStr);

            Elements skuEles = spuEle.select("li.ps-item");
            for (Element skuEle : skuEles) {
                try {
                    long sku = Long.parseLong(skuEle.select("[data-sku]").attr("data-sku"));

                    //根据sku查询商品是否存在
                    Item item = new Item();
                    item.setSku(sku);
                    if (itemService.findAll(item).size() > 0) {
                        continue;
                    }
                    item.setSpu(spu);

                    //详情
                    String itemUrl = "https://item.jd.com/" + sku + ".html";
                    item.setUrl(itemUrl);

                    //图片
                    String picUrl = "https:" + skuEle.select("img[data-sku]").first().attr("data-lazy-img");
                    if (picUrl.equals("https:")) continue;
                    picUrl = picUrl.replace("/n7/", "/n1/");
                    String picName = httpUtils.doGetImage(picUrl);
                    item.setPic(picName);

                    //价格
                    String priceJson = httpUtils.doGetHtml("https://p.3.cn/prices/mgets?skuId=J_" + sku);
                    double price = MAPPER.readTree(priceJson).get(0).get("p").asDouble();
                    item.setPrice(price);

                    //标题
                    String itemInfo = httpUtils.doGetHtml(item.getUrl());
                    String title = Jsoup.parse(itemInfo).select("div.sku-name").text();
                    item.setTitle(title);

                    //时间
                    item.setCreated(new Date());
                    item.setUpdated(item.getCreated());

                    itemService.save(item);
                } catch (Exception e) {
                    System.out.println("出错啦");
                    System.out.println(e.getMessage());
                    continue;
                }
            }
        }
    }

}
