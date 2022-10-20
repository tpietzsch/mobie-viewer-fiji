package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import org.embl.mobie.viewer.annotation.Annotation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcatenatedAnnotationTableModel< A extends Annotation > extends AbstractAnnotationTableModel< A >
{
	private final Set< AnnotationTableModel< A > > tableModels;
	private AnnotationTableModel< A > referenceTable;
	private Map< A, Integer > annotationToRowIndex = new ConcurrentHashMap<>();
	private Map< Integer, A > rowIndexToAnnotation = new ConcurrentHashMap<>();
	private AtomicInteger numAnnotations = new AtomicInteger( 0 );

	public ConcatenatedAnnotationTableModel( Set< AnnotationTableModel< A > > tableModels )
	{
		this.tableModels = tableModels;

		// Note that all loading of data from the {@code tableModels}
		// it handled by the listening
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.addAnnotationListener( this );

		this.referenceTable = tableModels.iterator().next();
	}

	private Map< A, Integer > getAnnotationToRowIndex()
	{
		return annotationToRowIndex;
	}

	@Override
	public List< String > columnNames()
	{
		return referenceTable.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return referenceTable.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return referenceTable.columnClass( columnName );
	}

	@Override
	public int numAnnotations()
	{
		return numAnnotations.get();
	}

	@Override
	public int rowIndexOf( A annotation )
	{
		return getAnnotationToRowIndex().get( annotation );
	}

	@Override
	public A annotation( int rowIndex )
	{
		// We do not update the tables here,
		// because one should only ask for
		// rows with an index lower than the
		// current numRows.
		return rowIndexToAnnotation.get( rowIndex );
	}

	@Override
	public void requestColumns( String columnsPath )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.requestColumns( columnsPath );
	}

	@Override
	public void setAvailableColumnPaths( Set< String > availableColumnPaths )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.setAvailableColumnPaths( availableColumnPaths );
	}

	@Override
	public Collection< String > availableColumnPaths()
	{
		return referenceTable.availableColumnPaths();
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return referenceTable.loadedColumnPaths();
	}

	@Override
	public Pair< Double, Double > getMinMax( String columnName )
	{
		return getColumnMinMax( columnName, annotations() );
	}

	@Override
	public Set< A > annotations()
	{
		return getAnnotationToRowIndex().keySet();
	}

	@Override
	public void addStringColumn( String columnName )
	{
		// here we probably need to load all tables
		throw new UnsupportedOperationException("Annotation of concatenated tables is not yet implemented.");
	}

	@Override
	public String dataStore()
	{
		return referenceTable.dataStore();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		for ( AnnotationTableModel< A > tableModel : tableModels )
			tableModel.transform( affineTransform3D );
	}

	@Override
	public void addAnnotationListener( AnnotationListener< A > listener )
	{
		listeners.add( listener );
		if( numAnnotations.get() > 0 )
			listener.addAnnotations( annotations() );
	}

	@Override
	public void addAnnotations( Collection< A > annotations )
	{
		for( A annotation : annotations )
			addAnnotation( annotation );
	}

	@Override
	public void addAnnotation( A annotation )
	{
		final int rowIndex = numAnnotations.incrementAndGet() - 1;
		annotationToRowIndex.put( annotation, rowIndex );
		rowIndexToAnnotation.put( rowIndex, annotation );

		for ( AnnotationListener< A > annotationListener : listeners.list )
			annotationListener.addAnnotation( annotation );
	}
}
