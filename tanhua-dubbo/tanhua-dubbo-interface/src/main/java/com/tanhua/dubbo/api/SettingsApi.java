package com.tanhua.dubbo.api;

import com.tanhua.model.domain.Settings;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-28 11:02
 */
public interface SettingsApi {
    Settings findByUserId(Long userId);

    void save(Settings settings);

    void update(Settings settings);
}

