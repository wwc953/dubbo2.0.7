/*
 * Copyright 1999-2011 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.container.standalone.StandaloneContainer;

/**
 * Main
 *
 * @author william.liangf
 */
public class Main {

    public static void main(String[] args) {
        //TODO SPI机制(2.0.7是java的spi)
        final Container container = ExtensionLoader.getExtensionLoader(Container.class).getAdaptiveExtension();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                container.stop();
            }
        });
        //TODO 解析Spring配置文件，以及Dubbo自定义标签
        container.start();
        synchronized (StandaloneContainer.class) {
            for (; ; ) {
                try {
                    StandaloneContainer.class.wait();
                } catch (Throwable e) {
                }
            }
        }
    }

}