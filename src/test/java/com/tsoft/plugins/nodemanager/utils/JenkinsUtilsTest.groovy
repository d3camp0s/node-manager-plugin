package com.tsoft.plugins.nodemanager.utils

import com.tsoft.plugins.nodemanager.config.NodeGroup
import hudson.util.CopyOnWriteMap
import net.sf.json.JSONObject
import org.junit.Before;

class JenkinsUtilsTest extends GroovyTestCase {

    def jsonString = "{'data':{'vca-venta':{'user':'dcampos02','label':'vca-venta','ignoreOffline':true,'date':'2020-04-08 14:17:117'},'vca-post-venta':{'user':'dcampos','label':'vca-post-venta','ignoreOffline':false,'date':'2020-04-08 19:15:810'}}}";;
    def jsonObject;
    def jenkinstestGroups = new CopyOnWriteMap.Hash()

    @Before
    void setUp() {
        jsonObject = JSONObject.fromObject(jsonString)
        jenkinstestGroups.put("vca",new NodeGroup("dcampos01", "vca", new Date(), true))
    }

    void testCopyOrUpdateGroups() {
        println(jenkinstestGroups.size())
        JenkinsUtils.copyOrUpdateGroups(jsonObject, jenkinstestGroups)
        println(jenkinstestGroups.size())
    }
}
