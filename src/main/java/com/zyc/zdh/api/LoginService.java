package com.zyc.zdh.api;

import com.zyc.zdh.shiro.MyAuthenticationToken;
import com.zyc.zdh.shiro.MyRealm;
import com.zyc.zdh.shiro.SessionDao;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zyc.zdh.entity.ResultInfo;
import com.zyc.zdh.entity.User;

import java.util.concurrent.TimeUnit;

/**
 * ClassName: LoginService
 *
 * @author zyc-admin
 * @date 2018年2月5日
 * @Description: cloud.api包下的服务 不需要通过shiro验证拦截，需要自定义的token验证
 */
@Controller("loginService")
@RequestMapping("api")
public class LoginService {


    @Autowired
    SessionDao sessionDao;
    @Autowired
    MyRealm myRealm;

    @RequestMapping("login")
    @ResponseBody
    public ResultInfo login(User user) {


        //在自己登录的rest里面写，比如UserRest里面的login方法中，user为传递过来的参数
        Subject currentUser = SecurityUtils.getSubject();
        MyAuthenticationToken token = new MyAuthenticationToken(user.getUserName(), user.getPassword(), false, "", "", "");
        // 开始进入shiro的认证流程
        currentUser.login(token);
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setStatus("200");
        resultInfo.setMessage("完成认证");
        resultInfo.setResult(currentUser.getSession());
        sessionDao.getRedisUtil().getRedisTemplate().expire(sessionDao.getCacheKey(currentUser.getSession().getId().toString()) , 7 * 24, TimeUnit.HOURS);

        return resultInfo;
    }

    @RequestMapping("test")
    @ResponseBody
    public ResultInfo test(String token) {

        ResultInfo resultInfo = new ResultInfo();

        resultInfo.setStatus("404");
        resultInfo.setMessage("请先完成认证");
        resultInfo.setResult("");
        if (valid(token)) {

            try{
                Session se=sessionDao.readSession(token);
                Object obj = se.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
                SimplePrincipalCollection coll = (SimplePrincipalCollection) obj;
                User user=(User) coll.getPrimaryPrincipal();

                if(user !=null) {
                    resultInfo.setStatus("200");
                    resultInfo.setMessage("以完成认证");
                    resultInfo.setResult(token);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return resultInfo;
    }

    @RequestMapping("logout")
    @ResponseBody
    public ResultInfo logout(String token) {

        ResultInfo resultInfo = new ResultInfo();
        if (valid(token)) {

            Session se=sessionDao.readSession(token);

            Object obj = se.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
            SimplePrincipalCollection coll = (SimplePrincipalCollection) obj;
            User user=(User) coll.getPrimaryPrincipal();

            if(user !=null){
                Cache<Object,AuthenticationInfo> cache=myRealm.getAuthenticationCache();
                if (cache!=null && user !=null){
                    cache.remove(user.getUserName());
                }
            }
            SecurityUtils.getSubject().logout();
            sessionDao.getActiveSessionsCache().remove(token);

            resultInfo.setStatus("200");
            resultInfo.setMessage("退出成功");
            resultInfo.setResult(token);
        } else {
            resultInfo.setStatus("404");
            resultInfo.setMessage("请先完成认证");
            resultInfo.setResult("");
        }
        return resultInfo;
    }


    /**
     * 验证token 是否有效
     * @param token
     * @return
     */
    private boolean valid(String token) {
        try {
            Session session = sessionDao.readSession(token);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}