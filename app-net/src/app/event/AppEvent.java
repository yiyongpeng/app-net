package app.event;

/**
 * 事件对象
 * 
 * @author yiyongpeng
 * 
 */
public class AppEvent {

	protected int type;

	protected Object attach;

	public AppEvent(int type) {
		this.type = type;
	}

	public AppEvent(int type, Object attach) {
		super();
		this.type = type;
		this.attach = attach;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getAttach() {
		return attach;
	}

	public void setAttach(Object attach) {
		this.attach = attach;
	}

}
