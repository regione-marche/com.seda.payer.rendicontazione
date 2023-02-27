package com.seda.payer.rendicontazione;

	public class ReturnCode {
		
	    private String codeErro;
	    private String messErro;
		
		public ReturnCode() {
			super();
		}

		public String getCodeErro() {
			return codeErro;
		}


		public void setCodeErro(String codeErro) {
			this.codeErro = codeErro;
		}


		public String getMessErro() {
			return messErro;
		}


		public void setMessErro(String messErro) {
			this.messErro = messErro;
		}
		
		public void setReturnCode(String codeErro, String messErro)
		{
			this.codeErro = codeErro;
			this.messErro = messErro;
		}

		@Override
		public String toString() {
			return "ReturnCode [codeErro=" + codeErro + ", messErro="
					+ messErro + "]";
		}
		
 
}
