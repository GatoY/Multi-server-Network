import cv2
import numpy as np
import math

def resize (img,height=None,width=None): #change the height, width of pics
    if height==None and width==None:
        return img
    #if inputs are none
    if height==None: #width!=None
        img=cv2.resize(img,(width,img.shape[0]),interpolation=cv2.INTER_AREA)
    if width==None:  #height!=None
        img=cv2.resize(img,(img.shape[1],height),interpolation=cv2.INTER_AREA)
    else: #width!=None,height!=None
        img=cv2.resize(img,(width,height),interpolation=cv2.INTER_AREA)
    return img

def gray(img): #change pics to gray mode
    return cv2.cvtColor(img,cv2.COLOR_BGR2GRAY)

def colorchange(img,r=0,g=0,b=0):
    for i in range(img.shape[0]):
        for j in range(img.shape[1]):
            if r<0:
                if img.item(i,j,0)+r>0:img.itemset((i,j,0),img.item(i,j,0)+r)
                else:img.itemset((i,j,0),0)
                print 1
            #rgb cannot lower than 0.
            else:
                if img.item(i,j,0)+r<255:img.itemset((i,j,0),img.item(i,j,0)+r)
                else:img.itemset((i,j,0),255)
                print 2
            #rgb cannot higher than 255.
            if g<0:
                if img.item(i,j,1)+g>0:img.itemset((i,j,1),img.item(i,j,1)+g)
                else:img.itemset((i,j,1),0)
                print 3
            else:
                if img.item(i,j,1)+g<255:img.itemset((i,j,1),img.item(i,j,1)+g)
                else:img.itemset((i,j,1),255)
                print 4
            if b<0:
                if img.item(i,j,2)+b>0:img.itemset((i,j,2),img.item(i,j,2)+b)
                else:img.itemset((i,j,2),0)
                print 5
            else:
                if img.item(i,j,2)+b<255:img.itemset((i,j,2),img.item(i,j,2)+b)
                else:img.itemset((i,j,2),255)
                print 6
    return img

def roi(img,x1,y1,x2,y2):
    #x1,y1 is the coordinates of mouse's first clicking
    #x2,y2 is of the second
    roi = img[y1:y2,x1:x2]
    return roi

def rotate(img,degree,addborder=1):
    height,width=img.shape[:2]
    maxlen=int(math.sqrt(height*height+width*width))+1
    if str(degree/90.0).isdigit==True:
        
    if addborder==1:
        img=cv2.copyMakeBorder(img,(maxlen-height)/2,(maxlen-height)/2,(maxlen-width)/2,(maxlen-width)/2,cv2.BORDER_CONSTANT,value=0)
    #add border to avoid lose some part of pic. But this import another question when rotate pic again.
    col,row=img.shape[:2]
    rot_mat=cv2.getRotationMatrix2D((row/2,col/2),degree,1)
    img=cv2.warpAffine(img,rot_mat,(row,col))
    return img
