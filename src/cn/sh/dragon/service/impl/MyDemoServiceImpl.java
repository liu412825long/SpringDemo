package cn.sh.dragon.service.impl;

import cn.sh.dragon.annotation.MyService;
import cn.sh.dragon.service.MyDemoService;

@MyService
public class MyDemoServiceImpl implements MyDemoService {

	@Override
	public void get(String name) {
		// TODO Auto-generated method stub
		System.out.println("My Name is "+name);
	}

}
