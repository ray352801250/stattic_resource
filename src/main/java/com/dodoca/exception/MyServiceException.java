
package com.dodoca.exception;

/**
 * @Author: TianGuangHui
 * @Date: 2019/3/14 18:43
 * @Description:
 *
 * */
public class MyServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 状态码
	 */
	private Integer code;

	public MyServiceException() {
		super();
	}

	public MyServiceException(String message) {
		super(message);
	}

	public MyServiceException(String message, Integer code){
		super(message);
		this.code = code;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

}
