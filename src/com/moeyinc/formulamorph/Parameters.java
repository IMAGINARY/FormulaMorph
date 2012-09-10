package com.moeyinc.formulamorph;

public interface Parameters {

	public enum Surface { F, M, G };

	public enum Name
	{
		F_a( Surface.F, 'a' ),
		F_b( Surface.F, 'b' ),
		F_c( Surface.F, 'c' ),
		F_d( Surface.F, 'd' ),
		F_e( Surface.F, 'e' ),
		F_f( Surface.F, 'f' ),
		
		M_t( Surface.M, 't' ),
		
		G_a( Surface.G, 'a' ),
		G_b( Surface.G, 'b' ),
		G_c( Surface.G, 'c' ),
		G_d( Surface.G, 'd' ),
		G_e( Surface.G, 'e' ),
		G_f( Surface.G, 'f' );

		private Surface surface;
		private char name;
		
		Name( Surface s, char name ) { surface = s; this.name = name; }
		public Surface getSurface() { return surface; }
		public char getName() { return name; }
	}
	
	public interface ActiveParameterListener
	{
		public void parameterStateChanged();
	}
	
	public void setParameterValue( Name name, double value );

	public void addActiveParameterListener( ActiveParameterListener apl );
	public void removeActiveParameterListener( ActiveParameterListener apl );
	
	public void setSurface( Surface surface, int index_in_gallery );
}
