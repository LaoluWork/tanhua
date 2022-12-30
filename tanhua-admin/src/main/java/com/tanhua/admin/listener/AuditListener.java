package com.tanhua.admin.listener;

import com.tanhua.autoconfig.template.AliyunGreenTemplate;
import com.tanhua.dubbo.api.MovementApi;
import com.tanhua.model.mongo.Movement;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-16 19:02
 */
@Component
public class AuditListener {

    @Autowired
    private AliyunGreenTemplate aliyunGreenTemplate;

    @DubboReference
    private MovementApi movementApi;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "tanhua.audit.queue",
                            durable = "true"
                    ),
                    exchange = @Exchange(
                            value = "tanhua.audit.exchange",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "audit.movement"
            )
    )
    public void audit(String movementId) {
        try {
            //1.查询出对应的movement
            Movement movement = movementApi.findById(movementId);
            if(movement != null && movement.getState() == 0) {
                //2.审核动态的文本，图片
                Map<String, String> textScan = aliyunGreenTemplate.greenTextScan(movement.getTextContent());
                Map<String, String> imageScan = aliyunGreenTemplate.imageScan(movement.getMedias());
                //3.判断审核结果
                int state = 0;
                if (textScan != null && imageScan != null) {
                    String textSuggestion = textScan.get("suggestion");
                    String imageSuggestion = imageScan.get("suggestion");
                    if("block".equals(textSuggestion) || "block".equals(imageSuggestion)){
                        state = 2; //驳回
                    }else if ("pass".equals(textSuggestion) && "pass".equals(imageSuggestion)){
                        state = 1; //通过
                    }
                }
                //4.修改动态的state
                movementApi.update(movementId, state);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
