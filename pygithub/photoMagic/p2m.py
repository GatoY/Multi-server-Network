import cv2
import numpy as numpy
import PhotoMagic

def addweight(img1,img2,alpha=0.5,beta=0.5,height=None,width=None):
    if height==None and width==None:
        height=500
        width=800
    if height!=None and width==None:
        width=img1.shape[1]
    if height==None and width!=None:
        height=img1.shape[0]
    img1=PhotoMagic.resize(img1,height,width)
    img2=PhotoMagic.resize(img2,height,width)
    #make img1,img2 get the same size for addweighted.
    img=cv2.addWeighted(img1,alpha,img2,beta,0)
    return img
