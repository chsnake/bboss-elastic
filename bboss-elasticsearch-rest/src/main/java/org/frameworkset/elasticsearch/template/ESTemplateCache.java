package org.frameworkset.elasticsearch.template;

import com.frameworkset.util.VariableHandler;
import org.frameworkset.util.annotations.DateFormateMeta;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class ESTemplateCache {
	private Lock lock = new ReentrantLock();
	private Lock vtplLock = new ReentrantLock();
	private Map<String,VariableHandler.URLStruction> parserTempateStructions = new java.util.HashMap<String,VariableHandler.URLStruction>();
	private Map<String,Map<String,VariableHandler.URLStruction>> parserVTPLTempateStructions = new java.util.HashMap<String,Map<String,VariableHandler.URLStruction>>();
	private static TempateStructionBuiler tempateStructionBuiler = new TempateStructionBuiler();
	public ESTemplateCache() {
		// TODO Auto-generated constructor stub
	}


	public void clear()
	{
		parserTempateStructions.clear();
		parserVTPLTempateStructions.clear();
	}


	public VariableHandler.URLStruction getTemplateStruction(ESInfo sqlinfo, String template)
	{
		if(sqlinfo.isTpl() )
		{
			return this._getVTPLTemplateStruction(sqlinfo,template);
		}
		else
		{
			return _getTemplateStruction(sqlinfo, template);
		}

//		VariableHandler.URLStruction sqlstruction =  _parserTempateStructions.get(key);
//		if(sqlstruction == null)
//		{
//			try
//			{
//				_lock.lock();
//				sqlstruction =  _parserTempateStructions.get(key);
//				if(sqlstruction == null)
//				{
//					sqlstruction = VariableHandler.parserTempateStruction(template);
//					_parserTempateStructions.put(key,sqlstruction);
//				}
//			}
//			finally {
//				_lock.unlock();
//			}
//		}
//		return sqlstruction;
	}

	public static class TempateVariable extends VariableHandler.Variable{
		/**
		 * 控制字符串变量是否需要添加""
		 * true 添加，默认添加
		 * false 不添加
		 * 模板变量的命名格式可以为：aaa,noquoted,dateformat=yyyy-MM-dd HH:mm:ss
		 */
		protected boolean quoted = true;
		protected String dateFormat ;
		protected String locale;
		protected String timeZone;
		protected DateFormateMeta dateFormateMeta;
		/**
		 * 在变量左边追加lpad对应的字符
		 */
		protected String lpad;
		/**
		 * 在变量的右边追加rpad对应的字符
		 */
		protected String rpad;
		public TempateVariable(){
			super();
		}

		/**
		 * 处理pad数据
		 * @param pad_
		 * @return
		 */
		private String handlePad(String pad_){
			int idx = pad_.indexOf("|");
			String pad = null;

			if(idx > 0 ){
				String value = pad_.substring(0,idx);
				int count = Integer.parseInt(pad_.substring(idx+1));
				pad = value;
				if(count > 0) {
					for (int j = 1; j < count; j++) {
						pad = pad + value;
					}
				}

			}
			else{
				pad = pad_;
			}
			return pad;
		}
		public void after(){
			super.after();
			if(this.variableName != null) {
				int pos = this.variableName.indexOf(",");
				if (pos > 0) {
					String[] ts = variableName.split(",");
					this.variableName = ts[0];
					for (int i = 1; i < ts.length; i ++) {
						String t = ts[i];
						if (t.startsWith("quoted=")) {
							String q = t.substring("quoted=".length()).trim();
							if(q.equals("false"))
								quoted = false;
						}
						else if(t.startsWith("dateformat=")){
							dateFormat= t.substring("dateformat=".length()).trim();
						}
						else if(t.startsWith("locale=")){
							locale= t.substring("locale=".length()).trim();
						}
						else if(t.startsWith("timezone=")){
							timeZone = t.substring("timezone=".length()).trim();
						}
						else if(t.startsWith("lpad=")){
							String lpad_= t.substring("lpad=".length()).trim();
							this.lpad = handlePad(lpad_);

						}
						else if(t.startsWith("rpad=")){
							String rpad_ = t.substring("rpad=".length()).trim();
							this.rpad = handlePad(rpad_);
						}
					}

					if(this.dateFormat != null){
						this.dateFormateMeta = DateFormateMeta.buildDateFormateMeta(this.dateFormat,this.locale);
					}

				}
			}
		}

		public boolean isQuoted() {
			return quoted;
		}

		public String getDateFormat() {
			return dateFormat;
		}

		public String getLocale() {
			return locale;
		}

		public DateFormateMeta getDateFormateMeta() {
			return dateFormateMeta;
		}

		public String getTimeZone() {
			return timeZone;
		}

		public String getLpad() {
			return lpad;
		}

		public String getRpad() {
			return rpad;
		}
	}
	static class TempateStructionBuiler extends VariableHandler.URLStructionBuiler {
		@Override
		public VariableHandler.Variable buildVariable() {
			return new TempateVariable();
		}

	}
	private VariableHandler.URLStruction _getTemplateStruction(ESInfo sqlinfo, String template)
	{

		String key = sqlinfo.getTemplateName();
		VariableHandler.URLStruction sqlstruction =  parserTempateStructions.get(key);
		if(sqlstruction == null)
		{
			try
			{
				lock.lock();
				sqlstruction =  parserTempateStructions.get(key);
				if(sqlstruction == null)
				{
					sqlstruction = VariableHandler.parserStruction(template,tempateStructionBuiler);
					parserTempateStructions.put(key,sqlstruction);
				}
			}
			finally {
				lock.unlock();
			}
		}
		return sqlstruction;
	}

	/**
	 * vtpl需要进行分级缓存
	 * @param sqlinfo
	 * @param template
	 * @return
	 */
	private VariableHandler.URLStruction _getVTPLTemplateStruction(ESInfo sqlinfo, String template)
	{

		String ikey = template;
		String okey = sqlinfo.getTemplateName();
		Map<String,VariableHandler.URLStruction> sqlstructionMap =  this.parserVTPLTempateStructions.get(okey);
		if(sqlstructionMap == null)
		{
			try
			{
				this.vtplLock.lock();
				sqlstructionMap =  this.parserVTPLTempateStructions.get(okey);
				if(sqlstructionMap == null)
				{
					sqlstructionMap = new   java.util.WeakHashMap<String,VariableHandler.URLStruction>();
					parserVTPLTempateStructions.put(okey,sqlstructionMap);
				}
			}
			finally {
				vtplLock.unlock();
			}
		}
		VariableHandler.URLStruction urlStruction = sqlstructionMap.get(ikey);
		if(urlStruction == null){
			try
			{
				this.vtplLock.lock();
				urlStruction = sqlstructionMap.get(ikey);
				if(urlStruction == null){
					urlStruction = VariableHandler.parserStruction(template,tempateStructionBuiler);
					sqlstructionMap.put(ikey,urlStruction);
				}
			}
			finally {
				this.vtplLock.unlock();
			}
		}
		return urlStruction;
	}

}
