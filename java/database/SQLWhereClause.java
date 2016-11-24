package com.zzbest.zzplatform.utils;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

public class SQLWhereClause {

	private StringBuilder whereBuffer = new StringBuilder();

	private SQLWhereClause() {
		whereBuffer.append(" where 1=1 ");
	}

	public static SQLWhereClause where() {
		return new SQLWhereClause();
	}

	public SQLWhereClause startSubClause() {
		this.whereBuffer.append(" ( ");

		return this;
	}

	public SQLWhereClause endSubClause() {
		this.whereBuffer.append(" ) ");

		return this;
	}

	public SQLWhereClause and() {
		this.whereBuffer.append(" and ");

		return this;
	}

	public SQLWhereClause or() {
		this.whereBuffer.append(" or ");

		return this;
	}

	public SQLWhereClause andLeftLike(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(" and " + field + " like '%" + value + "'");
		}

		return this;
	}

	public SQLWhereClause andLike(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(" and " + field + " like '%" + value + "%'");
		}

		return this;
	}

	public SQLWhereClause orLike(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(" or " + field + " like '%" + value + "%'");
		}

		return this;
	}

	public SQLWhereClause andEqual(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(" and " + field + " = '" + value + "'");
		}

		return this;
	}

	public SQLWhereClause leftLike(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(field + " like '%" + value + "'");
		}

		return this;
	}

	public SQLWhereClause rightLike(String field, String value) {
		this.whereBuffer.append(field + " like '" + value + "%'");

		return this;
	}

	public SQLWhereClause like(String field, String value) {
		if (canConcatenate(field, value)) {
			this.whereBuffer.append(field + " like '%" + value + "%'");
		}

		return this;
	}

	public String toString() {
		return this.whereBuffer.toString();
	}

	public boolean canConcatenate(String field, String value) {
		if (StringUtils.isBlank(field) || StringUtils.isBlank(value)) {
			return false;
		}

		return true;
	}

	public static String collectionToDelimitedString(Collection<?> coll) {
		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append("'").append(it.next()).append("'");
			if (it.hasNext()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public static String arrayToDelimitedString(Object[] arr) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("'").append(arr[i]).append("'");
		}
		return sb.toString();
	}
}
