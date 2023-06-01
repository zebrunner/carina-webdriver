package com.zebrunner.carina.webdriver.proxy;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.webdriver.proxy.mode.UpstreamMode;

public class ZebrunnerProxyTest {

    @Test
    public void testHeaderModify() {
        ZebrunnerProxyBuilder.getInstance()
                .addHeaderModify(FilterConditions.header("^Date.*$"), "MODIFIED DATE", "MODIFIED J@()fh////N@(8h0")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--modify-headers \":~h ^Date.*$:MODIFIED DATE:MODIFIED J@()fh////N@(8h0\"");
    }

    @Test
    public void testHeaderModifyWithDoubleQuotes() {
        ZebrunnerProxyBuilder.getInstance()
                .addHeaderModify(FilterConditions.header("^Date\\s.*$"), "MODI\"FIED DATE\"", "MODIFIED J@()fh////N@(8h0\"")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--modify-headers \":~h ^Date\\s.*$:MODI\\\"FIED DATE\\\":MODIFIED J@()fh////N@(8h0\\\"\"");
    }

    @Test
    public void testHeaderModifyWithAnd() {
        ZebrunnerProxyBuilder.getInstance()
                .addHeaderModify(FilterConditions.and(
                        FilterConditions.header("^Date.*$"),
                        FilterConditions.responseCode(302)), "Date",
                        "MODIFIED J@()fh////N@(8h0\"")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--modify-headers \":(~h ^Date.*$ & ~c 302):Date:MODIFIED J@()fh////N@(8h0\\\"\"");
    }

    @Test
    public void testBodyModifyWithOr() {
        ZebrunnerProxyBuilder.getInstance()
                .addBodyModify(FilterConditions.or(FilterConditions.body("301\\sMoved\\sPermanently"),
                        FilterConditions.method("POST")), ".*", "MODIFIED/*\"BODY********")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--modify-body \":(~b 301\\sMoved\\sPermanently | ~m POST):.*:MODIFIED/*\\\"BODY********\"");
    }

    @Test
    public void testUpstreamMode() {
        ZebrunnerProxyBuilder.getInstance()
                .addMode(new UpstreamMode("https://host:port"))
                .upstreamAuth("admin", "pass")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--mode upstream:https://host:port --upstream-auth admin:pass");
    }

    @Test
    public void testMapRemote() {
        ZebrunnerProxyBuilder.getInstance()
                .addMapRemote(".*[jpg|png| jpeg]$", "https://example.com")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--map-remote \"%.*[jpg|png| jpeg]$%https://example.com\"");
    }

    @Test
    public void testStickyCookie() {
        ZebrunnerProxyBuilder.getInstance()
                .stickyCookie(FilterConditions.header("Cache-Control\\sno-cache"))
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--stickycookie \"~h Cache-Control\\sno-cache\"");
    }

    @Test
    public void testStickyAuth() {
        ZebrunnerProxyBuilder.getInstance()
                .stickyAuth(FilterConditions.or(FilterConditions.header("Cache-Control:\\sno-cache"), FilterConditions.body("^test")))
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--stickyauth \"(~h Cache-Control:\\sno-cache | ~b ^test)\"");
    }

    @Test
    public void testFlowDetail() {
        ZebrunnerProxyBuilder.getInstance()
                .flowDetail(ZebrunnerProxyBuilder.FlowDetail.FULL_URL_RESPONSE_CODE_HEADERS_FULL_CONTENT)
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--flow-detail 4");
    }

    @Test
    public void testListenHost() {
        ZebrunnerProxyBuilder.getInstance()
                .listenHost("127.0.0.1")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--listen-host 127.0.0.1");
    }

    @Test
    public void testListenPort() {
        ZebrunnerProxyBuilder.getInstance()
                .listenPort(55555)
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--listen-port 55555");
    }

    @Test
    public void testIgnoreHost() {
        ZebrunnerProxyBuilder.getInstance()
                .addIgnoreHost("(^(.+\\.)?examp\"le\\.org)|(^(.+\\.)?example2\\.dev)|(^(.+\\.)?example3\\.com)")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--ignore-hosts \"(^(.+\\.)?examp\\\"le\\.org)|(^(.+\\.)?example2\\.dev)|(^(.+\\.)?example3\\.com)\"");
    }

    @Test
    public void testAllowHost() {
        ZebrunnerProxyBuilder.getInstance()
                .addAllowHost(".*")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--allow-hosts \".*\"");
    }

    @Test
    public void testTCPHost() {
        ZebrunnerProxyBuilder.getInstance()
                .addTCPHost("(^(.+\\.)?examp\"le\\.org)|(^(.+\\.)?example\"2\\.dev)|(^(.+\\.)?example\\\\\"3\\.com)")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--tcp-hosts \"(^(.+\\.)?examp\\\"le\\.org)|(^(.+\\.)?example\\\"2\\.dev)|(^(.+\\.)?example\\\"3\\.com)\"");
    }

    @Test
    public void testUpstreamProxyWithAuth() {
        ZebrunnerProxyBuilder.getInstance()
                .addMode(new UpstreamMode("127.0.0.1"))
                .upstreamAuth("admin", "1234")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--mode upstream:127.0.0.1 --upstream-auth admin:1234");
    }

    @Test
    public void testProxyAuth() {
        ZebrunnerProxyBuilder.getInstance()
                .proxyAuth("admin", "password")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--proxyauth admin:password");
    }

    @Test
    public void testBodyModifyWithoutFilterCondition() {
        ZebrunnerProxyBuilder.getInstance()
                .addBodyModify(".*", "MODIFIED/*\"BODY********")
                .build(true);
        Assert.assertEquals(R.CONFIG.get(ZebrunnerProxyBuilder.PROXY_ARGUMENTS_PARAMETER).trim(),
                "--modify-body \":.*:MODIFIED/*\\\"BODY********\"");
    }
}
