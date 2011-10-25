package com.gigaspaces.example;

import java.util.Collection;

public interface ITestService {

    public IResult add( Collection<IParameter> parameters );

    public IResult add( IParameter parameter );
    
    public IResult add( IParameter [] parameters );
    
   public IResult modify( Collection<IParameter> parameters, IParameter parameter2 );

    public IResult modify( IParameter parameter2, Collection<IParameter> parameters );

    public IResult modify( IParameter parameter1, IParameter parameter2 );
    
}
