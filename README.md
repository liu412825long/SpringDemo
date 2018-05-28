# SpringDemo
Java web的一个实现Spring IOC 的Demo
实现步骤：
1：自定义一个DispatcherServlet
2: 把DispatcherServlet配置到web.xml中，并配置好访问路径
3：加载Spring 的配置文件
4：初始化Bean，并将其存放到Map中
5：扫描相关的类，告诉Spring 哪些是需要被初始化的
6：默认的beanName,为key进行实例化Bean
7: 自动给类的属性赋值，即对 @Autowired 赋值
8：保存url和method对应的关系
9：调用doGet,doPost
10：拿到url去mapping中取值
11：调用method.invoke() 得到一个返回值
12：response输出