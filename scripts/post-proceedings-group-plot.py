import os
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import re
import argparse

def get_curve_label(filename):
    patterns = {
        r'.*GT.*': 'Ground truth',
        r'^pyramisCDF.*?_ts(\d+(?:\.\d+)?)\.txt$': lambda m: f'Pyramis - time tick {m.group(1)}',
    }
    for pattern, label_func in patterns.items():
        match = re.match(pattern, filename)
        if match:
            if callable(label_func):
                return label_func(match)
            else:
                return label_func
    return filename

def get_curve_style(filename):
    if 'GT' in filename:
        return {'color': 'black', 'linestyle': '--', 'linewidth': 2}
    elif 'ts0.1' in filename:
        return {'color': '#0077BB', 'linewidth': 2}
    elif 'ts0.2' in filename:
        return {'color': '#009988', 'linewidth': 2}
    elif 'ts0.4' in filename:
        return {'color': '#CC3311', 'linewidth': 2}

def load_cdf_data(filepath):
    return pd.read_csv(filepath, delim_whitespace=True, header=None, names=['x', 'y'])

def is_gt_file(filename):
    return bool(re.match(r'^\d*GT_', filename))

def plot_cdfs_from_folder(folder, parallelValue, seqValue, alternativeValue, gt_folder=None, gt_number=None):
    expID = f"PAR{parallelValue}_SEQ{seqValue}_ALT{alternativeValue}"
    
    # Costruire il nome del file GT in base al parametro gt_number
    if gt_number is not None:
        gt_filename = f"{gt_number}GT_{expID}.txt"
    else:
        gt_filename = f"GT_{expID}.txt"
    
    filenames = [
        gt_filename,
        f'pyramisCDF_{expID}_ts0.4.txt',
        f'pyramisCDF_{expID}_ts0.2.txt',
        f'pyramisCDF_{expID}_ts0.1.txt'
    ]
    
    curves = []
    plt.figure(figsize=(10, 6))
    plt.rcParams.update({'font.size': 22})
    plt.rc('axes', labelsize=22)
    plt.rc('legend', fontsize=20)
    plt.rc('xtick', labelsize=20)
    plt.rc('ytick', labelsize=20)
    global_max_x = 0
    
    # Prima passata: trova il massimo valore di x globale
    for filename in filenames:
        if is_gt_file(filename) and gt_folder is not None:
            file_path = os.path.join(gt_folder, filename)
        else:
            file_path = os.path.join(folder, filename)
            
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            max_x = df['x'].max()
            global_max_x = max(global_max_x, max_x)
    
    # Second Step: plot
    for filename in filenames:
        if is_gt_file(filename) and gt_folder is not None:
            file_path = os.path.join(gt_folder, filename)
        else:
            file_path = os.path.join(folder, filename)
            
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            style = get_curve_style(filename)
            curve = plt.plot(df['x'], df['y'],
                           label=get_curve_label(filename), 
                           **style)
            curves.append((curve[0], get_curve_label(filename)))
    
    if curves:  # Solo se abbiamo trovato almeno una curva
        plt.xlim(0, global_max_x * 1.05)
        plt.xlabel('time')
        plt.ylabel('response time distribution')
        plt.ylim(0, 1)
        handles, labels = zip(*curves)
        plt.legend(handles, labels, loc='lower right')
        plt.grid(True)
        plt.tight_layout()
        plt.savefig(os.path.join(folder, "HCDF_comparison" + expID + ".png"))
        plt.savefig(os.path.join(folder, "HCDF_comparison" + expID + ".pdf"))
        plt.close()
    else:
        print(f"Warning: No data files found for experiment {expID}")
        plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Plot CDF from four files in the same folder.")
    parser.add_argument("folder", help="path to the folder containing the data files")
    parser.add_argument("--gt-folder", help="path to the folder containing the GT files (optional, defaults to main folder)")
    parser.add_argument("--gt-number", type=int, help="number to prefix GT files (optional, e.g. 123GT_*.txt)")
    args = parser.parse_args()
    
    combinations = []
    defaults = {'PAR': 2, 'SEQ': 2, 'ALT': 2}
    single_combinations = [4, 8]
    
    for param in defaults.keys():
        for value in single_combinations:
            combinations.append((param, value))
    
    plot_cdfs_from_folder(args.folder, 2, 2, 2, gt_folder=args.gt_folder, gt_number=args.gt_number) 
    
    for i, (param_name, param_value) in enumerate(combinations, 1):
        kwargs = defaults.copy()
        kwargs[param_name] = param_value
        plot_cdfs_from_folder(args.folder, kwargs['PAR'], kwargs['SEQ'], kwargs['ALT'], gt_folder=args.gt_folder, gt_number=args.gt_number)
