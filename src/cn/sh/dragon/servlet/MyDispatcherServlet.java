package cn.sh.dragon.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.security.auth.message.callback.PrivateKeyCallback.Request;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.sh.dragon.annotation.MyAutowired;
import cn.sh.dragon.annotation.MyController;
import cn.sh.dragon.annotation.MyRequestMapping;
import cn.sh.dragon.annotation.MyService;

public class MyDispatcherServlet extends HttpServlet {
	
	private Properties contextConfig=new Properties();
	
	private List<String> classNames=new ArrayList<>();
	
	private Map<String,Object> iocMap= new HashMap<String, Object>();
	
	private Map<String,Method> handlerMapping=new HashMap<>();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doPost(req, resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//6:等待请求
		doDispatch( req,  resp);
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
		// TODO Auto-generated method stub
		String uri=req.getRequestURI();
		String contextPath=req.getContextPath();
		uri=uri.replace(contextPath, "").replaceAll("/+", "//");
		try {
		if(handlerMapping.containsKey(uri)) {
			resp.getWriter().write("404 Not found!");
			return;
			
		}
		Method method=handlerMapping.get(uri);
//		method.invoke(obj, args)
		System.out.println(handlerMapping.get(uri));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		System.out.println("Servlet 初始化完毕！");
		
		//1:加载配置文件
		doLoadContextConfig(config.getInitParameter("contextLocationConfig"));
		
		//2：扫描所有相关的类
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3：实例化所有相关的类到IOC容器中
		doInstance();
		
		//4：完成自动化注入的过程
		doAutowired();
		
		//5：初始化HandlerMapping
		initHandlerMapping();
	}

	private void initHandlerMapping() {
		// TODO Auto-generated method stub
		if(iocMap.isEmpty()) return;
		for(Entry< String, Object> entry:iocMap.entrySet()) {
			Class<?> class1=entry.getValue().getClass();
			if(!class1.isAnnotationPresent(MyController.class)) {
				continue;
			}
			
			if(class1.isAnnotationPresent(MyRequestMapping.class)) {
				MyRequestMapping myRequestMapping=class1.getAnnotation(MyRequestMapping.class);
				String baseUrl=myRequestMapping.value().trim();
				
				//获取所有共有的方法，而不是class1.getDeclaredMethods()
				Method[] methods=class1.getMethods();
				
				for(Method method:methods) {
					if(!method.isAnnotationPresent(MyRequestMapping.class)) continue;
					
					MyRequestMapping requestMapping=method.getAnnotation(MyRequestMapping.class);
					String url="/"+baseUrl+"/" + requestMapping.value().trim();
					url=url.replaceAll("/+", "/");
					System.out.println(url+","+method);
					handlerMapping.put(url, method);
				}
				
			}
			
			
		}
		
	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		if(iocMap.isEmpty()) return;
		for(Entry<String, Object> entry:iocMap.entrySet()) {
			
			Field[] fields=entry.getValue().getClass().getDeclaredFields();
			for(Field field:fields) {
				if(!field.isAnnotationPresent(MyAutowired.class)) continue;
				
				MyAutowired autowired=field.getAnnotation(MyAutowired.class);
				String beanName=autowired.value();
				if("".equals(beanName)) {
					beanName=field.getType().getName();
				}
				field.setAccessible(true);
				try {
					field.set(entry.getValue(),iocMap.get(beanName));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void doInstance() {
		// TODO Auto-generated method stub
		if(classNames.isEmpty()) {
			return;
		}
		try {
			for(String className:classNames) {
				Class<?> class1=Class.forName(className);
				//加了自定义注解的类才可以实例化 
				if(class1.isAnnotationPresent(MyController.class)) {
					//把对象存到一个Map中，而这个Map需要由key-value
					//1:默认雷鸣首字母小写
					//2：自定义
					//3：自动识别接口类型作为key
					Object object=class1.newInstance();
					String key=lowerFiestCase(class1.getSimpleName());
					iocMap.put(key, object);
					
				}else if(class1.isAnnotationPresent(MyService.class)) {
					MyService myService=class1.getAnnotation(MyService.class);
					String beanName=myService.value();
					if("".equals(beanName.trim())) {
						beanName=lowerFiestCase(class1.getSimpleName());
					}
					Object object=class1.newInstance();
					iocMap.put(beanName, object);
					
					//3：自动识别接口类型作为key
					Class<?>[] interfaces=class1.getInterfaces();
					for(Class<?> class2:interfaces) {
						iocMap.put(class2.getName(), object);
					}
					
				}
				
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String lowerFiestCase(String string) {
		char[] chars=string.toCharArray();
		chars[0]+=32;
		return String.valueOf(chars);
	}

	private void doScanner(String basePackage) {
		// TODO Auto-generated method stub
		//这里的 basePackage.replaceAll("\\.", "/") 而不是basePackage.replaceAll(".", "/")
		URL url=this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));
		File classDir=new File(url.getFile());
		for(File file:classDir.listFiles()) {
			if(file.isDirectory()) {
				doScanner(basePackage+"."+file.getName());
			}else {
				if(file.getName().endsWith(".class")) {
				String className=basePackage+"."+file.getName().replace(".class", "");
				classNames.add(className);
				}
			}
			
		}
	}

	private void doLoadContextConfig(String configLocation) {
		// TODO Auto-generated method stub
		InputStream iStream=getClass().getClassLoader()
				.getResourceAsStream(configLocation);
		try {
			contextConfig.load(iStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(null != iStream) {
				try {
					iStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	
}
