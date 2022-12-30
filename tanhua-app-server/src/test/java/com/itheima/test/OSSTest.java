package com.itheima.test;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.tanhua.autoconfig.template.OssTemplate;
import com.tanhua.server.AppServerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-13 15:53
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AppServerApplication.class)
public class OSSTest {

    @Autowired
    private OssTemplate ossTemplate;

    @Test
    public void testOssTemplate() throws FileNotFoundException {
        //1、配置图片路径
        String path = "D:\\老卢\\Saved Pictures\\11.jpg";
        //2、构造FileInputStream
        FileInputStream inputStream = new FileInputStream(path);
        String url = ossTemplate.upload(path, inputStream);
        System.out.println(url);
    }

    @Test
    public void testOss() throws FileNotFoundException {
        //1、配置图片路径
        String path = "D:\\老卢\\Saved Pictures\\20.jpg";
        //2、构造FileInputStream
        FileInputStream inputStream = new FileInputStream(path);
        //3、瓶邪图片在Oss的路径
        String fileName = new SimpleDateFormat("yyyy/MM/dd").format(new Date())
                +"/"+ UUID.randomUUID() + path.substring(path.lastIndexOf("."));
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-guangzhou.aliyuncs.com";
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = "LTAI5tKLDcMRtJ7QyQKzMeGx";
        String accessKeySecret = "oOS9EJPtVHphFqPJIO3p5ozFRZYeD6";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        String url = "https://tan--hua001.oss-cn-guangzhou.aliyuncs.com/" + fileName;
        System.out.println(url);

        try {
            // 创建PutObject请求。
            ossClient.putObject("tan--hua001", fileName, inputStream);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
