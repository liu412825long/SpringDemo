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
		//6:�ȴ�����
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
		System.out.println("Servlet ��ʼ����ϣ�");
		
		//1:���������ļ�
		doLoadContextConfig(config.getInitParameter("contextLocationConfig"));
		
		//2��ɨ��������ص���
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3��ʵ����������ص��ൽIOC������
		doInstance();
		
		//4������Զ���ע��Ĺ���
		doAutowired();
		
		//5����ʼ��HandlerMapping
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
				
				//��ȡ���й��еķ�����������class1.getDeclaredMethods()
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
				//�����Զ���ע�����ſ���ʵ���� 
				if(class1.isAnnotationPresent(MyController.class)) {
					//�Ѷ���浽һ��Map�У������Map��Ҫ��key-value
					//1:Ĭ����������ĸСд
					//2���Զ���
					//3���Զ�ʶ��ӿ�������Ϊkey
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
					
					//3���Զ�ʶ��ӿ�������Ϊkey
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
		//����� basePackage.replaceAll("\\.", "/") ������basePackage.replaceAll(".", "/")
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
