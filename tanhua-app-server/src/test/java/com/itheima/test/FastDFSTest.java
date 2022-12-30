package com.itheima.test;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.autoconfig.template.OssTemplate;
import com.tanhua.server.AppServerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
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
public class FastDFSTest {

    @Autowired
    private FastFileStorageClient client;

    @Autowired
    private FdfsWebServer webServer;

    @Test
    public void testUpload() throws FileNotFoundException {
        // 1.指定文件位置
        File file = new File("D:\\老卢\\Saved Pictures\\5.jpg");
        // 2.文件上传
        StorePath path = client.uploadFile(new FileInputStream(file), file.length(), "jpg", null);
        // 3.拼接请求路径打印
        String fullPath = path.getFullPath();
        System.out.println(fullPath);
        String url = webServer.getWebServerUrl() + fullPath;
        System.out.println(url);

    }
}
