package com.d1m.wechat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tk.mybatis.mapper.common.Mapper;

import com.d1m.wechat.mapper.MassConversationResultMapper;
import com.d1m.wechat.model.MassConversationResult;
import com.d1m.wechat.service.MassConversationResultService;

@Service
public class MassConversationResultServiceImpl extends
		BaseService<MassConversationResult> implements
		MassConversationResultService {

	@Autowired
	private MassConversationResultMapper massConversationResultMapper;

	@Override
	public Mapper<MassConversationResult> getGenericMapper() {
		return massConversationResultMapper;
	}

	@Override
	public MassConversationResult getByMsgId(Integer wechatId, String msgId) {
		MassConversationResult record = new MassConversationResult();
		record.setWechatId(wechatId);
		record.setMsgId(msgId);
		return massConversationResultMapper.selectOne(record);
	}

	@Override
	public MassConversationResult getByConversationId(Integer wechatId, Integer conversationId) {
		MassConversationResult record = new MassConversationResult();
		record.setWechatId(wechatId);
		record.setConversationId(conversationId);
		return massConversationResultMapper.selectOne(record);
	}
}
