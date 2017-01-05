package com.zld.struts.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.service.DataBaseService;
import com.zld.struts.parkadmin.MemberManageAction;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
/*˵���������̻������������ŵ���Ա����������һ�����ﴦ�������ͬһ���࣬�����̻���¼������Ա������
�����̻�����Ӫ���Ź�����Ҳ����Ա��������Ϊ���ӵ�ַ��ͬ����Ȩ��ʱ��ͻ���ң�������ÿһ����֯���Ͷ���һ����Ա������*/
public class ChanMemberManageAction extends Action {
	@Autowired
	private DataBaseService daService;
	@Autowired
	private CommonMethods commonMethods;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		Integer supperadmin = (Integer)request.getSession().getAttribute("supperadmin");//�Ƿ��ǳ�������Ա
		Integer isAdmin =(Integer)request.getSession().getAttribute("isadmin");//�Ƿ��ǹ���Ա
		Long uin = (Long)request.getSession().getAttribute("loginuin");//��¼���û�id
		Long roleid = (Long)request.getSession().getAttribute("loginroleid");//��¼���û�role_id
		Long chanid = (Long)request.getSession().getAttribute("chanid");
		request.setAttribute("authid", request.getParameter("authid"));
		if(uin == null){
			response.sendRedirect("login.do");
			return null;
		}
		if(supperadmin == 1){
			chanid = RequestUtil.getLong(request, "chanid", -1L);
		}
		Map<String, Object> adminRoleMap = daService.getMap("select * from user_role_tb where type=? and oid =(select id from zld_orgtype_tb where name like ? limit ? ) limit ? ", 
				new Object[]{0, "%����%", 1, 1});//������������Ա��ɫ
		if(adminRoleMap == null || chanid < 0){
			return null;
		}
		if(action.equals("")){
			request.setAttribute("chanid", chanid);
			return mapping.findForward("list");
		}else if(action.equals("getrole")){
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			if(supperadmin == 1){
				Map<String, Object> roleMap = new HashMap<String, Object>();
				roleMap.put("value_no", adminRoleMap.get("id"));
				roleMap.put("value_name", adminRoleMap.get("role_name"));
				list.add(0,roleMap);
			}else{
				String sql = "select id as value_no,role_name as value_name from user_role_tb where state=? and oid=? and adminid=? ";
				list = daService.getAll(sql, new Object[]{0, adminRoleMap.get("oid"), uin});
			}
			String result = "[]";
			if(list != null && !list.isEmpty()){
				result = StringUtils.createJson(list);
			}
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("quickquery")){
			String sql = "select u.* from user_info_tb u,user_role_tb r where u.role_id=r.id and u.chanid=? and u.state=? and r.state=? and r.oid=? ";
			String countSql = "select count(u.*) from user_info_tb u,user_role_tb r where u.role_id=r.id and u.chanid=? and u.state=? and r.state=? and r.oid=? ";
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			List list = null;
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
			List<Object> params = new ArrayList<Object>();
			params.add(chanid);
			params.add(0);
			params.add(0);
			params.add(adminRoleMap.get("oid"));
			if(supperadmin == 1){//�ܹ���Աֻ�ܿ�������Ա
				sql += " and u.role_id = ? ";
				countSql += " and u.role_id = ? ";
				params.add(adminRoleMap.get("id"));
			}else{//���ܹ���Ա��¼���ܿ�������Ա,��ֻ�ܿ����Լ������Ľ�ɫ�µ���Ա
				sql += " and u.role_id <> ? and adminid=? ";
				countSql += " and u.role_id <> ? and adminid=? ";
				params.add(adminRoleMap.get("id"));
				params.add(uin);
			}
			Long count = daService.getCount(countSql, params);
			if(count > 0){
				list = daService.getAll(sql, params, pageNum, pageSize);
			}
			String json = JsonUtil.Map2Json(list,pageNum,count, fieldsstr,"id");
			AjaxUtil.ajaxOutput(response, json);
			return null;
		}else if(action.equals("create")){
			int result = createMember(request, chanid, uin);
			AjaxUtil.ajaxOutput(response, result + "");
		}else if(action.equals("edit")){
			int result = editMember(request);
			AjaxUtil.ajaxOutput(response, result + "");
		}else if(action.equals("delete")){
			int result = deleteMember(request);
			AjaxUtil.ajaxOutput(response, result + "");
		}else if(action.equals("editpass")){
			String result = editPass(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("check")){
			String strid = RequestUtil.processParams(request, "value");
			String sql = "select count(*) from user_info_tb where strid =?";
			Long result = daService.getLong(sql, new Object[]{strid});
			if(result>0)
				AjaxUtil.ajaxOutput(response, "1");
			else {
				AjaxUtil.ajaxOutput(response, "0");
			}
		}
		return null;
	}
	
	//ע��ͣ�����շ�Ա�ʺ�
	@SuppressWarnings({ "rawtypes" })
	private int createMember(HttpServletRequest request, Long chanid, Long creator_id){
		String strid =RequestUtil.processParams(request, "strid");
		String nickname =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "nickname"));
		String phone =RequestUtil.processParams(request, "phone");
		String mobile =RequestUtil.processParams(request, "mobile");
		Long role_id =RequestUtil.getLong(request, "role_id", -1L);
		if(role_id == -1){
			return -1;
		}
		if(nickname.equals("")) nickname=null;
		if(phone.equals("")) phone=null;
		if(mobile.equals("")) mobile=null;
		Long time = System.currentTimeMillis()/1000;
		if(!commonMethods.checkStrid(strid))
			return 0;
		//�û���
		String sql="insert into user_info_tb (nickname,password,strid,reg_time,mobile,phone,chanid,role_id,auth_flag,creator_id) " +
				"values (?,?,?,?,?,?,?,?,?,?)";
		Object [] values= new Object[]{nickname,strid,strid,time,mobile,phone,chanid,role_id,-1,creator_id};
		int r = daService.update(sql, values);
		return r;
	}
	
	private int editMember(HttpServletRequest request){
		Long id = RequestUtil.getLong(request, "id", -1L);
		String nickname =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "nickname"));
		String strid =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "strid"));
		String phone =RequestUtil.processParams(request, "phone");
		String mobile =RequestUtil.processParams(request, "mobile");
		Integer role_id = RequestUtil.getInteger(request, "role_id", -1);
		if(role_id == -1 || id == -1){
			return -1;
		}
		if(nickname.equals("")) nickname=null;
		if(phone.equals("")) phone=null;
		if(mobile.equals("")) mobile=null;
		if(!commonMethods.checkStrid(strid, id)){
			return -2;
		}
		int r = daService.update("update user_info_tb set nickname=?,strid=?,phone=?,mobile=?,role_id=? where id=? ", 
				new Object[]{nickname, strid, phone, mobile, role_id, id});
		return r;
	}
	
	private int deleteMember(HttpServletRequest request){
		Long id = RequestUtil.getLong(request, "selids", -1L);
		int r = daService.update("update user_info_tb set state=? where id=? ", new Object[]{1, id});
		return r;
	}
	
	@SuppressWarnings({ "rawtypes" })
	private String editPass(HttpServletRequest request){
		String id =RequestUtil.processParams(request, "id");
		String sql = "update user_info_tb set password =? ,md5pass=? where id =?";
		String newPass = RequestUtil.processParams(request, "newpass");
		String confirmPass = RequestUtil.processParams(request, "confirmpass");
		String md5pass = newPass;
		try {
			md5pass = StringUtils.MD5(newPass);
			md5pass = StringUtils.MD5(md5pass+"zldtingchebao201410092009");
		} catch (Exception e) {
			e.printStackTrace();
		}
		String result = "0";
		if(newPass.length()<6){
			result = "���볤��С��6λ�����������룡";
		}else if(newPass.equals(confirmPass)){
			Object [] values = new Object[]{newPass,md5pass,Long.valueOf(id)};
			int r = daService.update(sql, values);
			result = r + "";
		}else {
			result = "�����������벻һ�£����������룡";
		}
		return result;
	}
}