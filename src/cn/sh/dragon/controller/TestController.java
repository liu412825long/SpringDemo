package cn.sh.dragon.controller;


import cn.sh.dragon.annotation.MyAutowired;
import cn.sh.dragon.annotation.MyController;
import cn.sh.dragon.annotation.MyRequestMapping;
import cn.sh.dragon.annotation.MyRequestParam;
import cn.sh.dragon.annotation.MyResponseBody;
import cn.sh.dragon.service.MyDemoService;

@MyController
@MyRequestMapping(value="/springDemo")
public class TestController {
	
	@MyAutowired
	private MyDemoService myDemoService;
	
	
	@MyRequestMapping(value="/index")
	@MyResponseBody
	public String index(@MyRequestParam("name")String name) {
		myDemoService.get(name);
		return "index:"+name;
	}

}
