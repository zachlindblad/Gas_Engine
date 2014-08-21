package com.glear.gas.resource;

/**
 * Created by biscuit on 8/20/2014.
 */
public abstract class Resource
{
    private LoadState loadState;
    private ResourceLoader loader;

    public void request(int requestingId)
    {
        loader.load(this);
    }

}
